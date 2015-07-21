package upsin.siptest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;
import android.util.Log;

public class IncomingCallReceiver extends BroadcastReceiver {
    public IncomingCallReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        SipAudioCall incomingCall = null;
        try{
            SipAudioCall.Listener listener = new SipAudioCall.Listener(){
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try{
                        call.answerCall(30);;
                    }catch (Exception ex){
                        Log.i("ERRORCALL", ex.getMessage());
                    }
                }
            };

            MainActivity activity = (MainActivity)context;

            incomingCall = activity.mSipManager.takeAudioCall(intent, listener);
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);
            if(incomingCall.isMuted()){
                incomingCall.toggleMute();
            }

            activity.mSipAudioCall = incomingCall;
            activity.updateAction(incomingCall);
        }catch (Exception ex){
            if(incomingCall != null)
                incomingCall.close();
        }
    }
}
