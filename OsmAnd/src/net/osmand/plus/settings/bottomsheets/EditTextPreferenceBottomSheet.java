package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.util.Algorithms;

import studio.carbonylgroup.textfieldboxes.TextFieldBoxes;

public class EditTextPreferenceBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = EditTextPreferenceBottomSheet.class.getSimpleName();

	private static final String EDIT_TEXT_PREFERENCE_KEY = "edit_text_preference_key";

	private EditText editText;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		EditTextPreferenceEx editTextPreference = getEditTextPreference();
		if (ctx == null || editTextPreference == null) {
			return;
		}

		items.add(new TitleItem(editTextPreference.getDialogTitle().toString()));
		String text;
		if (savedInstanceState != null) {
			text = savedInstanceState.getString(EDIT_TEXT_PREFERENCE_KEY);
		} else {
			text = editTextPreference.getText();
		}

		View view = UiUtilities.getInflater(ctx, nightMode).inflate(R.layout.preference_edit_text_box, null);
		editText = view.findViewById(R.id.edit_text);
		editText.setText(text);

		RelativeLayout editTextLayout = view.findViewById(R.id.text_field_boxes_editTextLayout);
		if (editTextLayout != null && editTextLayout.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) editTextLayout.getLayoutParams();
			params.setMargins(params.leftMargin, AndroidUtils.dpToPx(ctx, 19), params.rightMargin, params.bottomMargin);
		}

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(view).create());

		String description = editTextPreference.getDescription();
		if (!Algorithms.isEmpty(description)) {
			TextFieldBoxes textFieldBoxes = view.findViewById(R.id.text_field_box);
			textFieldBoxes.setHelperText(description);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(EDIT_TEXT_PREFERENCE_KEY, editText.getText().toString());
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		EditTextPreferenceEx editTextPreference = getEditTextPreference();
		if (editTextPreference != null) {
			String value = editText.getText().toString();
			if (editTextPreference.callChangeListener(value)) {
				editTextPreference.setText(value);

				Fragment target = getTargetFragment();
				if (target instanceof OnPreferenceChanged) {
					((OnPreferenceChanged) target).onPreferenceChanged(editTextPreference.getKey());
				}
			}
		}

		dismiss();
	}

	private EditTextPreferenceEx getEditTextPreference() {
		return (EditTextPreferenceEx) getPreference();
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target,
									   boolean usedOnMap, @Nullable ApplicationMode appMode) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			EditTextPreferenceBottomSheet fragment = new EditTextPreferenceBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}