package moe.mal.waifus.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.Status;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import moe.mal.waifus.Ougi;
import moe.mal.waifus.R;
import moe.mal.waifus.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AuthActivity {

    private static final int RC_SAVE = 1;

    @BindView(R.id.usernameField) EditText usernameField;
    @BindView(R.id.passwordField) EditText passwordField;
    @BindView(R.id.loginButton) Button loginButton;
    @BindView(R.id.signUpButton) Button signUpButton;

    private String username;
    private String password;

    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        if (Ougi.getInstance().getUser().isLoggedIn()) {
            usernameField.setText(Ougi.getInstance().getUser().getUsername());
            passwordField.setText(Ougi.getInstance().getUser().getPassword());
        }

        loginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loginPressed();
                    }
                }
        );

        signUpButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        signUpPressed();
                    }
                }
        );
    }

    /**
     * Helper method that takes a response and parses it
     * @param response the Response instance that is generated from the API call
     * @param login true if the call was made from login, false if from sign up
     */
    private void handleServerResponse(Response<User> response, boolean login) {
        if ((response == null) || (response.code() != 200)) {
            if (login) {
                showToast("Those credentials weren't correct.");
            } else {
                showToast("Sign up failed. Are you already registered?");
            }
            progress.dismiss();
            return;
        }

        // Create a Credential with the user's email as the ID and storing the password.  We
        // could also add 'Name' and 'ProfilePictureURL' but that is outside the scope of this
        // minimal sample.
        final Credential credential = new Credential.Builder(username)
                .setPassword(password)
                .build();

        User user = response.body();
        user.setCredential(credential);
        Ougi.getInstance().setUser(user);

        // NOTE: this method unconditionally saves the Credential built, even if all the fields
        // are blank or it is invalid in some other way.  In a real application you should contact
        // your app's back end and determine that the credential is valid before saving it to the
        // Credentials backend.
        Auth.CredentialsApi.save(mCredentialsApiClient, credential).setResultCallback(
                new ResolvingResultCallbacks<Status>(this, RC_SAVE) {
                    @Override
                    public void onSuccess(Status status) {
                    }

                    @Override
                    public void onUnresolvableFailure(Status status) {
                    }
                });
        progress.dismiss();

        if (Ougi.getInstance().needToScrape()) {
            moveApp(ScrapeActivity.class, "url", Ougi.getInstance().popScrapingURL());
        } else {
            moveApp(SadActivity.class);
        }
    }

    /**
     * Helper method to validate user credentials
     * @return true if valid
     */
    public boolean validate() {
        boolean valid = true;

        if (!verifyGenericInput(password)) {
            passwordField.setError("enter a valid password");
            passwordField.requestFocus();
            valid = false;
        } else {
            passwordField.setError(null);
        }

        if (!(verifyGenericInput(username) && verifyInputWithRegex(username, USERNAME_REGEX))) {
            usernameField.setError("enter a valid username");
            usernameField.requestFocus();
            valid = false;
        } else {
            usernameField.setError(null);
        }

        return valid;
    }


    /**
     * Method to be executed when the login button is pressed
     */
    public void loginPressed() {
        username = usernameField.getText().toString();
        password = passwordField.getText().toString();

        if (!validate()) {
            return;
        }

        showProgress("Trying to log you in.");

        Call<User> call = Ougi.getInstance().getWaifuAPI()
                .getUserInfo(username, User.buildAuth(username, password));

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                handleServerResponse(response, true);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                handleServerResponse(null, true);
            }
        });
    }

    /**
     * Method to be executed when the sign up button is pressed
     */
    public void signUpPressed() {
        Map<String, String> args = new HashMap<>();

        username = usernameField.getText().toString();
        password = passwordField.getText().toString();

        if (verifyGenericInput(username)) {
            args.put("username", username);
        }

        if (verifyGenericInput(password)) {
            args.put("password", password);
        }

        showScreen(SignUpActivity.class);
    }

    /**
     * Private helper method to show a progress dialog
     * @param message the message to be displayed
     */
    private void showProgress(String message) {

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        progress = new ProgressDialog(this);
        progress.setTitle("Please Wait");
        progress.setMessage(message);
        progress.setCancelable(true);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();
    }

}
