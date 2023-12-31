package prototype.xd.scheduler.fragments;

import static prototype.xd.scheduler.utilities.Utilities.displayToast;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.SlidePolicy;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.views.CheckBox;

public final class IntroStartingFragment extends Fragment implements SlidePolicy { // NOSONAR, this is a fragment
    
    private CheckBox understoodCheckbox;
    
    @NonNull
    @Override
    @MainThread
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intro_starting_fragment, container, false);
        understoodCheckbox = view.findViewById(R.id.understood_checkbox);
        return view;
    }
    
    @Override
    public boolean isPolicyRespected() {
        return understoodCheckbox.isChecked();
    }
    
    @Override
    public void onUserIllegallyRequestedNextPage() {
        displayToast(requireContext(), R.string.please_read_the_notes);
    }
}
