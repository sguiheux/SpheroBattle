package fr.sgu.spherobattle.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.sgu.spherobattle.R;

public class AccueilFragment extends Fragment {

    private OnAccueilFragmentListener broadcaster;

    /**
     * Creation du fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_accueil, container, false);
        // Launch game
        rootView.findViewById(R.id.main_button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcaster.runGame();
            }
        });

        // Exit game
        rootView.findViewById(R.id.main_button_quit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcaster.exitGame();
            }
        });
        return rootView;
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);

        if (ctx instanceof OnAccueilFragmentListener) {
            broadcaster = (OnAccueilFragmentListener) ctx;
        }
    }

    public interface OnAccueilFragmentListener {
        void runGame();

        void exitGame();
    }
}
