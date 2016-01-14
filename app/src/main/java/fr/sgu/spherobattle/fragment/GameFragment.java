package fr.sgu.spherobattle.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.orbotix.ConvenienceRobot;
import com.orbotix.Ollie;
import com.orbotix.Sphero;
import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.calibration.api.CalibrationEventListener;
import com.orbotix.calibration.api.CalibrationImageButtonView;
import com.orbotix.calibration.api.CalibrationView;
import com.orbotix.classic.DiscoveryAgentClassic;
import com.orbotix.classic.RobotClassic;
import com.orbotix.colorpicker.api.ColorPickerEventListener;
import com.orbotix.colorpicker.api.ColorPickerFragment;
import com.orbotix.command.RGBLEDOutputCommand;
import com.orbotix.common.DiscoveryAgentEventListener;
import com.orbotix.common.DiscoveryAgentProxy;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.ResponseListener;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.common.internal.AsyncMessage;
import com.orbotix.common.internal.DeviceResponse;
import com.orbotix.common.sensor.LocatorData;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.joystick.api.JoystickEventListener;
import com.orbotix.joystick.api.JoystickView;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.le.RobotLE;
import com.orbotix.robotpicker.RobotPickerDialog;
import com.orbotix.subsystem.SensorControl;

import java.util.Date;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import fr.sgu.spherobattle.R;

public class GameFragment extends Fragment implements RobotPickerDialog.RobotPickerListener, DiscoveryAgentEventListener, RobotChangedStateListener {

    private OnGameFragmentListener broadcaster;

    private boolean spheroConnected = false;
    private boolean opponentReady = false;

    // ////////////////////////////////////   View
    // Joystick
    private JoystickView _joystick;
    private ColorPickerFragment _colorPicker;
    private Button _colorPickerButton;

    private TextView vitesseTV;

    private DiscoveryAgentProxy _currentDiscoveryAgent;
    private RobotPickerDialog.RobotPicked robotPicked;
    private RobotPickerDialog _robotPickerDialog;
    private AlertDialog alertDialog;

    //Robot connected
    private ConvenienceRobot mRobot;


    /**
     * Creation du fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_game, container, false);
        vitesseTV = (TextView) rootView.findViewById(R.id.game_vitesse);
        setupJoystick(rootView);
        setupCalibration(rootView);
        setupColorPicker(rootView);

        broadcaster.connectAndLaunchMulti();

        chooseRobot();

        return rootView;
    }

    /**
     * Sets up the calibration gesture and button
     */
    private void setupCalibration(View v) {
        CalibrationView _calibrationView = (CalibrationView) v.findViewById(R.id.calibrationView);
        _calibrationView.setShowGlow(true);
        _calibrationView.setCalibrationEventListener(new CalibrationEventListener() {

            @Override
            public void onCalibrationBegan() {
                mRobot.calibrating(true);
            }

            @Override
            public void onCalibrationChanged(float angle) {
                mRobot.rotate(angle);
            }

            /**
             * Invoked when the user stops the calibration process
             */
            @Override
            public void onCalibrationEnded() {
                mRobot.stop();
                mRobot.calibrating(false);
            }
        });

        CalibrationImageButtonView _calibrationButtonView = (CalibrationImageButtonView) v.findViewById(R.id.calibrateButton);
        _calibrationButtonView.setCalibrationView(_calibrationView);
    }

    /**
     * Sets up the joystick from scratch
     */
    private void setupJoystick(View v) {
        _joystick = (JoystickView) v.findViewById(R.id.joystickView);
        _joystick.setJoystickEventListener(new JoystickEventListener() {

            @Override
            public void onJoystickBegan() {
            }

            @Override
            public void onJoystickMoved(double distanceFromCenter, double angle) {
                mRobot.drive((float) angle, 1f);
            }

            @Override
            public void onJoystickEnded() {
            }
        });
    }

    private void setupColorPicker(View v) {
        _colorPicker = new ColorPickerFragment();
        _colorPicker.setColorPickerEventListener(new ColorPickerEventListener() {
            @Override
            public void onColorPickerChanged(int red, int green, int blue) {
                mRobot.sendCommand(new RGBLEDOutputCommand(red, green, blue, false));
                _colorPickerButton.setBackgroundColor(Color.rgb(red, green, blue));
                //getFragmentManager().popBackStack();
                //getFragmentManager().popBackStackImmediate();
                //getFragmentManager().beginTransaction().remove(_colorPicker).commit();
            }

            @Override
            public void validate() {
                _colorPickerButton.setVisibility(View.VISIBLE);
                getFragmentManager().beginTransaction().remove(_colorPicker).commit();
            }
        });

        // Find the color picker fragment and add a click listener to show the color picker
        _colorPickerButton = (Button) v.findViewById(R.id.colorPickerButton);
        _colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _colorPickerButton.setVisibility(View.GONE);
                android.support.v4.app.FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.fragment_root, _colorPicker, "ColorPicker");
                transaction.show(_colorPicker);
                transaction.addToBackStack("DriveSample");
                transaction.commit();
            }
        });

    }


    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);

        if (ctx instanceof OnGameFragmentListener) {
            broadcaster = (OnGameFragmentListener) ctx;
        }
    }

    public void start() {
        if (spheroConnected) {
            broadcaster.sendReady();
        }
    }

    public void opponentReady() {
        opponentReady = true;
        toggleControl();
    }

    private void toggleControl() {
        if (opponentReady && spheroConnected) {
            Crouton.makeText(getActivity(),"GO GO GO",Style.CONFIRM).show();
            _joystick.setEnabled(true);
        } else {
            _joystick.setEnabled(false);
        }
    }

    private void chooseRobot() {
        if (_robotPickerDialog == null) {
            _robotPickerDialog = new RobotPickerDialog(getContext(), this);
        }
        if (!_robotPickerDialog.isShowing()) {
            _robotPickerDialog.show();
        }
    }

    @Override
    public void handleRobotsAvailable(List<Robot> robots) {
        if (_currentDiscoveryAgent instanceof DiscoveryAgentClassic) {
            _currentDiscoveryAgent.connect(robots.get(0));
        }
    }

    @Override
    public void onRobotPicked(RobotPickerDialog.RobotPicked robotPicked) {
        _robotPickerDialog.dismiss();
        this.robotPicked = robotPicked;
        initDiscovery();
    }

    private void initDiscovery() {
        switch (this.robotPicked) {
            case Sphero:
                _currentDiscoveryAgent = DiscoveryAgentClassic.getInstance();
                break;
            case Ollie:
                _currentDiscoveryAgent = DiscoveryAgentLE.getInstance();
                break;
        }
        startDiscovery();
    }

    /**
     * Starts discovery on the set discovery agent and look for robots
     */
    private void startDiscovery() {
        if (!_currentDiscoveryAgent.isDiscovering()) {
            try {
                AlertDialog.Builder adBuilder = new AlertDialog.Builder(getContext());
                adBuilder.setTitle("Connecting...");
                alertDialog = adBuilder.show();
                _currentDiscoveryAgent.addDiscoveryListener(this);
                _currentDiscoveryAgent.addRobotStateListener(this);
                _currentDiscoveryAgent.startDiscovery(getContext());
            } catch (DiscoveryException e) {
                Crouton.makeText(getActivity(), "Could not start discovery. Reason: " + e.getMessage(), Style.ALERT).show();
            }
        }

    }

    @Override
    public void handleRobotChangedState(Robot robot, RobotChangedStateListener.RobotChangedStateNotificationType type) {
        switch (type) {
            case Online:
                spheroConnected = true;
                alertDialog.dismiss();
                _currentDiscoveryAgent.stopDiscovery();
                _currentDiscoveryAgent.removeDiscoveryListener(this);

                // Bluetooth Classic (Sphero)
                if (robot instanceof RobotClassic) {
                    mRobot = new Sphero(robot);
                }
                // Bluetooth LE (Ollie)
                if (robot instanceof RobotLE) {
                    mRobot = new Ollie(robot);
                }
                broadcaster.sendReady();
                toggleControl();
                break;
            case Disconnected:
                spheroConnected = false;
                toggleControl();
                break;
        }
    }

    /**
     * Init after sphero connected
     */
    private void init() {
        long sensorFlag = SensorFlag.VELOCITY.longValue();
        mRobot.enableSensors(sensorFlag, SensorControl.StreamingRate.STREAMING_RATE10);
        mRobot.enableCollisions(true);

        // robot listener
        mRobot.addResponseListener(new ResponseListener() {
            @Override
            public void handleResponse(DeviceResponse deviceResponse, Robot robot) {

            }

            @Override
            public void handleStringResponse(String s, Robot robot) {

            }

            @Override
            public void handleAsyncMessage(AsyncMessage asyncMessage, Robot robot) {
                if (asyncMessage instanceof CollisionDetectedAsyncData) {

                } else if (asyncMessage instanceof DeviceSensorAsyncMessage) {
                    if (((DeviceSensorAsyncMessage) asyncMessage).getAsyncData() != null) {
                        LocatorData locatorData = ((DeviceSensorAsyncMessage) asyncMessage).getAsyncData().get(0).getLocatorData();
                        float speedX = locatorData.getVelocityX();
                        float speedY = locatorData.getVelocityY();
                        double speed = Math.sqrt(Math.pow(speedX, 2) + Math.pow(speedY, 2));
                        vitesseTV.setText(speed+" cm/s");
                    }
                }
            }
        });

    }

    public interface OnGameFragmentListener {
        void connectAndLaunchMulti();

        void sendReady();
    }
}
