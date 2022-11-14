package prototype.xd.scheduler.views.lockscreen;

import android.util.TypedValue;
import android.view.View;

import prototype.xd.scheduler.databinding.BasicEntryBinding;

public class BasicLockScreenTodoItemView extends LockScreenTodoItemView<BasicEntryBinding> {
    
    public BasicLockScreenTodoItemView(BasicEntryBinding binding) {
        super(binding);
    }
    
    @Override
    public void setBackgroundColor(int color) {
        viewBinding.backgroundMain.setBackgroundColor(color);
    }
    
    @Override
    public void setBorderColor(int color) {
        viewBinding.backgroundOutline.setBackgroundColor(color);
    }
    
    @Override
    public void setTitleTextColor(int color) {
        viewBinding.titleText.setTextColor(color);
    }
    
    @Override
    public void setIndicatorColor(int color) {
        viewBinding.indicatorView.setBackgroundColor(color);
    }
    
    @Override
    public void setBorderSize(int sizePX) {
        viewBinding.backgroundOutline.setPadding(sizePX, sizePX, sizePX, sizePX);
    }
    
    @Override
    public void setTitleText(String text) {
        viewBinding.titleText.setText(text);
    }
    
    @Override
    public void setTitleTextSize(float sizeDP) {
        viewBinding.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDP);
    }
    
    @Override
    public void setTimeText(String text) {
        viewBinding.timeText.setText(text);
    }
    
    @Override
    public void setTimeTextSize(float sizeDP) {
        viewBinding.timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDP);
    }
    
    @Override
    public void hideIndicatorAndTime() {
        viewBinding.timeText.setVisibility(View.GONE);
        viewBinding.indicatorView.setVisibility(View.GONE);
    }
}
