package upsin.siptest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;


public class MainActivity extends ActionBarActivity {


    public String sipAddress = null;

    public SipProfile mSipProfile = null;
    public SipManager mSipManager = null;
    public SipAudioCall mSipAudioCall = null;
    public IncomingCallReceiver callReceiver;

    private static final int CALL_ADDRESS = 1;
    private static final int SET_AUTH_INFO = 2;
    private static final int UPDATE_SETTINGS_DIALOG = 3;
    private static final int HANG_UP = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

        initializeManager();
    }

    public void initializeManager() {
        if (mSipManager == null) {
            mSipManager = SipManager.newInstance(this);
        }

        initializeProfile();
    }


    @Override
    protected void onStart() {
        super.onStart();
        initializeManager();
    }

    public void initializeProfile(){
        if(mSipManager == null)
            return ;
        if(mSipProfile == null)
            closeLocalProfile();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = preferences.getString("username", "");
        String password = preferences.getString("password","");
        String domain = preferences.getString("domain","");

        if(username.length() == 0 || domain.length() == 0 || password.length() == 0){
            showDialog(UPDATE_SETTINGS_DIALOG);
        }

        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            mSipProfile = builder.build();

            mSipManager.setRegistrationListener(mSipProfile.getUriString(), new SipRegistrationListener() {
                @Override
                public void onRegistering(String localProfileUri) {
                    updateAction("Registrando");
                }

                @Override
                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    updateAction("Registrado");
                }

                @Override
                public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                    updateAction("Fallo el registro");
                }
            });

            Intent intent = new Intent();
            intent.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);
            mSipManager.open(mSipProfile, pendingIntent, null);

        }catch (ParseException ex){
            Log.i("Fallo 1","" + ex.getMessage());
        }catch(SipException ex){
            Log.i("Fallo 2","" + ex.getMessage());
        }

    }

    public void updateAction(final String message){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView)findViewById(R.id.sipLabel);
                textView.append(message + "\n");
            }
        });
    }
    public void updateAction(final SipAudioCall call){
        String useName = call.getPeerProfile().getDisplayName();
        if(useName == null){
            useName = call.getPeerProfile().getUserName();
        }

        updateAction(useName + "@" + call.getPeerProfile().getSipDomain());
    }
    public void closeLocalProfile(){
        try {
            if (mSipManager == null) {
                return;
            } else {
                if (mSipProfile != null) {
                    mSipManager.close(mSipProfile.getUriString());
                }
            }
        }catch (SipException ex){
            Log.i("Fallo 3","Fallo al cerrar" + ex.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSipAudioCall != null)
            mSipAudioCall.close();
        closeLocalProfile();
        if(callReceiver != null)
            this.unregisterReceiver(callReceiver);


    }

    public void openPreference(){
        Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
        startActivity(intent);
    }
    public void initiateCall(){
        updateAction(sipAddress);
        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onCalling(SipAudioCall call) {
                    updateAction("Llamando");
                }

                @Override
                public void onCallEstablished(SipAudioCall call) {
                    call.startAudio();
                    call.setSpeakerMode(true);
                    call.toggleMute();
                    updateAction(call);
                    updateAction("Llamando...");
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    updateAction("Llamada terminada...");
                }
            };
            mSipAudioCall = mSipManager.makeAudioCall(mSipProfile.getUriString(), sipAddress, listener, 30);
        }catch(SipException ex){
            if(mSipProfile != null){
                try{
                    mSipManager.close(mSipProfile.getUriString());
                }catch(Exception sEx){
                    Log.i("ERRORLLAMADA", "Error tratando de cerrar el manager", sEx);
                    sEx.printStackTrace();
                }
            }
            if(mSipAudioCall != null)
                mSipAudioCall.close();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALL_ADDRESS, 0, "Llamar a alguien");
        menu.add(0, SET_AUTH_INFO, 0, "Editar preferencias");
        menu.add(0, HANG_UP, 0, "Terminar llamada");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){

            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case SET_AUTH_INFO:
                openPreference();
                break;
        }
        return true;
    }

    protected Dialog onCreateDialog(int id){
        switch (id){
            case CALL_ADDRESS:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.call_address_dialog, null);
                return new AlertDialog.Builder(this)
                        .setTitle("Llamar a alguien")
                        .setView(textBoxView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText textField = (EditText)(textBoxView.findViewById(R.id.calladdress_edit));
                                sipAddress = textField.getText().toString();
                                initiateCall();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create();
            case UPDATE_SETTINGS_DIALOG:
                return new AlertDialog.Builder(this).setMessage("Por favor actualiza tu cuenta SIP")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openPreference();
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create();
        }

        return null;
    }


}
