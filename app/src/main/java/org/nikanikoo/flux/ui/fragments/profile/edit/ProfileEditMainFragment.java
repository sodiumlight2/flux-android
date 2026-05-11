package org.nikanikoo.flux.ui.fragments.profile.edit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;
import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.managers.api.OpenVKApi;

public class ProfileEditMainFragment extends Fragment {

    private ShapeableImageView avatarImage;
    private MaterialButton changeAvatarButton;
    private TextInputEditText nameEdit;
    private TextInputEditText surnameEdit;
    private TextInputEditText nicknameEdit;
    private TextInputEditText statusEdit;
    private TextInputEditText homeTownEdit;
    private RadioGroup sexRadioGroup;
    private MaterialRadioButton sexMale;
    private MaterialRadioButton sexFemale;
    private AutoCompleteTextView relationAutoComplete;
    private TextInputEditText bdateEdit;
    private RadioGroup bdateVisibilityRadioGroup;
    private MaterialRadioButton bdateVisibilityFull;
    private MaterialRadioButton bdateVisibilityDayMonth;
    private MaterialButton saveButton;

    private final String[] relationOptions = {
            "Не выбрано", // 0
            "Не женат/Не замужем", // 1
            "Встречаюсь", // 2
            "Помолвлен(а)", // 3
            "Женат/Замужем", // 4
            "В гражданском браке", // 5
            "Влюблен(а)", // 6
            "Всё сложно", // 7
            "В активном поиске" // 8
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_edit_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupClickListeners();
        loadProfileData();
    }

    private void initViews(View view) {
        avatarImage = view.findViewById(R.id.avatar_image);
        changeAvatarButton = view.findViewById(R.id.change_avatar_button);
        nameEdit = view.findViewById(R.id.name_edit);
        surnameEdit = view.findViewById(R.id.surname_edit);
        nicknameEdit = view.findViewById(R.id.nickname_edit);
        statusEdit = view.findViewById(R.id.status_edit);
        homeTownEdit = view.findViewById(R.id.home_town_edit);
        sexRadioGroup = view.findViewById(R.id.sex_radio_group);
        sexMale = view.findViewById(R.id.sex_male);
        sexFemale = view.findViewById(R.id.sex_female);
        relationAutoComplete = view.findViewById(R.id.relation_auto_complete);
        bdateEdit = view.findViewById(R.id.bdate_edit);
        bdateVisibilityRadioGroup = view.findViewById(R.id.bdate_visibility_radio_group);
        bdateVisibilityFull = view.findViewById(R.id.bdate_visibility_full);
        bdateVisibilityDayMonth = view.findViewById(R.id.bdate_visibility_day_month);
        saveButton = view.findViewById(R.id.save_button);

        ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, relationOptions);
        relationAutoComplete.setAdapter(relationAdapter);
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> {
            // TODO: сохранить изменения профиля
            Toast.makeText(requireContext(), "Изменения сохранены", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadProfileData() {
        OpenVKApi.getInstance(requireContext()).callMethod("account.getProfileInfo", null, new OpenVKApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                try {
                    JSONObject responseObj = response.getJSONObject("response");

                    String firstName = responseObj.optString("first_name");
                    String lastName = responseObj.optString("last_name");
                    String photoUrl = responseObj.optString("photo_200");
                    String nickname = responseObj.optString("nickname");
                    String status = responseObj.optString("status");
                    String homeTown = responseObj.optString("home_town");
                    String bdate = responseObj.optString("bdate");
                    int sex = responseObj.optInt("sex", 0);
                    int relation = responseObj.optInt("relation", 0);
                    int bdateVisibility = responseObj.optInt("bdate_visibility", 0);

                    nameEdit.setText(firstName);
                    surnameEdit.setText(lastName);
                    nicknameEdit.setText(nickname);
                    statusEdit.setText(status);
                    homeTownEdit.setText(homeTown);
                    bdateEdit.setText(bdate);

                    if (sex == 1) {
                        sexFemale.setChecked(true);
                    } else if (sex == 2) {
                        sexMale.setChecked(true);
                    }

                    if (relation >= 0 && relation < relationOptions.length) {
                        relationAutoComplete.setText(relationOptions[relation], false);
                    }

                    if (bdateVisibility == 1) {
                        bdateVisibilityFull.setChecked(true);
                    } else if (bdateVisibility == 2) {
                        bdateVisibilityDayMonth.setChecked(true);
                    }

                    if (!photoUrl.isEmpty()) {
                        Picasso.get().load(photoUrl).placeholder(R.drawable.ic_account).into(avatarImage);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Ошибка парсинга данных", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}