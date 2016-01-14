package fr.sgu.spherobattle;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import fr.sgu.spherobattle.fragment.AccueilFragment;
import fr.sgu.spherobattle.fragment.GameFragment;
import fr.sgu.spherobattle.model.Message;
import fr.sgu.spherobattle.model.TypeMessage;

public class MainActivity extends AppCompatActivity implements RealTimeMessageReceivedListener, RoomUpdateListener, GameFragment.OnGameFragmentListener, AccueilFragment.OnAccueilFragmentListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleApiClient;

    /// GOOGLE PLAY SERVICES ERROR MANAGEMENT
    private boolean mResolvingError;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    public int REQUEST_RESOLVE_ERROR = 1001;

    // GOOGLE MULTIPLAYER DATA
    private static final int RC_WAITING_ROOM = 666;
    private String mRoomId;
    private Room mRoom;
    private String myParticipantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        switchFragment(new AccueilFragment());

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        // Create a GoogleApiClient instance
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Games.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Check Permission
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }
    }

    /**
     * Result for location permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if ( ! (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!googleApiClient.isConnecting() &&
                        !googleApiClient.isConnected()) {
                    googleApiClient.connect();
                }
            }
        } else if (requestCode == RC_WAITING_ROOM) {
            if (resultCode == Activity.RESULT_OK) {
                for (Fragment f : getSupportFragmentManager().getFragments()) {
                    if (f instanceof GameFragment && f.isVisible()) {
                        ((GameFragment) f).start();
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Games.RealTimeMultiplayer.leave(googleApiClient, null, mRoomId);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // player wants to leave the room.
                Games.RealTimeMultiplayer.leave(googleApiClient, null, mRoomId);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    public void launchMulti() {
        Bundle am = RoomConfig.createAutoMatchCriteria(1, 1, 0);

        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this).setMessageReceivedListener(this);
        roomConfigBuilder.setAutoMatchCriteria(am);
        RoomConfig roomConfig = roomConfigBuilder.build();

        // create room:
        Games.RealTimeMultiplayer.create(googleApiClient, roomConfig);

        // prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    //////////////////   GOOGLE PLAY SERVICES LISTENER

    @Override
    public void onConnected(Bundle bundle) {
        Crouton.makeText(this, "Connected to Google Play service", Style.INFO).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Crouton.makeText(this, "Connection to Google Play service Suspended", Style.ALERT);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (!mResolvingError && result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                googleApiClient.connect();
            }
        } else if (!mResolvingError) {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            Crouton.makeText(this, "Connection to Google Play service failed :" + result.getErrorCode(), Style.ALERT);
            mResolvingError = true;
        }
    }

    /////////////////   FRAGMENT LISTeNER

    @Override
    public void connectAndLaunchMulti() {
        if (!mResolvingError) {
            if (googleApiClient.isConnected()) {
                launchMulti();
            } else {
                googleApiClient.connect();
            }
        }
    }

    @Override
    public void sendReady() {
        if(mRoom != null){
            for (Participant p : mRoom.getParticipants()) {
                if (!p.getParticipantId().equals(myParticipantId)) {
                    Message msg = new Message();
                    msg.type = TypeMessage.PRESENCE;
                    msg.data = "1";
                    Games.RealTimeMultiplayer.sendReliableMessage(googleApiClient, null, Message.toByte(msg),
                            mRoomId, p.getParticipantId());
                }
            }
        }
    }

    @Override
    public void runGame() {
        switchFragment(new GameFragment());
    }

    @Override
    public void exitGame() {
        finish();
    }

    ///////////////    ROOM LISTENER

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Crouton.makeText(this, "Failed to create the game.", Style.ALERT).show();
            return;
        }

        if (room != null) {
            mRoomId = room.getRoomId();
            mRoom = room;
        }
        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, Integer.MAX_VALUE);
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Lorsque qu'un rejoint la partie.
    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Crouton.makeText(this, "Failed to join the game.", Style.ALERT).show();
            switchFragment(new AccueilFragment());
            return;
        }

        if (room != null) {
            mRoomId = room.getRoomId();
            mRoom = room;
            myParticipantId = room.getParticipantId(Games.Players.getCurrentPlayerId(googleApiClient));
        }
        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, Integer.MAX_VALUE);
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    @Override
    public void onLeftRoom(int statusCode, String s) {

    }

    @Override
    public void onRoomConnected(int i, Room room) {
        // TOUS LES JOUEURS SONT LA
        if (room != null) {
            mRoom = room;
            mRoomId = room.getRoomId();
        }
    }

    //////////////////   Echange de msg pendant la partie
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
        Message msg = Message.toMessage(realTimeMessage.getMessageData());
        for(Fragment f : getSupportFragmentManager().getFragments()){
            if (f.isVisible() && f instanceof GameFragment){
                switch(msg.type){
                    case PRESENCE:
                        ((GameFragment) f).opponentReady();
                        break;
                    case COLLISION:
                        break;
                }
            }
        }
    }
}
