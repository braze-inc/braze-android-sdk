package com.appboy.sample

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import com.appboy.sample.dialog.CustomDialogBase
import com.appboy.sample.util.ButtonUtils
import com.braze.Braze.Companion.getInstance
import com.braze.BrazeUser
import com.braze.enums.Gender
import com.braze.enums.Month.Companion.getMonth
import com.braze.support.BrazeLogger.getBrazeLogTag
import java.util.Calendar

class UserProfileDialog : CustomDialogBase(), View.OnClickListener {
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var gender: RadioGroup
    private lateinit var language: EditText
    private lateinit var birthdayText: TextView
    private lateinit var phoneNumber: EditText

    private lateinit var datePickerDialog: DatePickerDialog
    private var birthYear = 0
    private var birthMonth = 0
    private var birthDay = 0
    private var isBirthdaySet = false

    private val sharedPreferences: SharedPreferences
        get() = requireContext().getSharedPreferences(getString(R.string.shared_prefs_location), Context.MODE_PRIVATE)

    private val birthdayDisplayValue: String
        get() = (birthMonth + 1).toString() + "/" + birthDay + "/" + birthYear

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.user_preferences, container, false)
        firstName = view.findViewById(R.id.first_name)
        lastName = view.findViewById(R.id.last_name)
        email = view.findViewById(R.id.email)
        gender = view.findViewById(R.id.gender)
        language = view.findViewById(R.id.language)
        birthdayText = view.findViewById(R.id.birthday)
        phoneNumber = view.findViewById(R.id.phone_number)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = sharedPreferences
        firstName.setText(sharedPreferences.getString(FIRST_NAME_PREFERENCE_KEY, null))
        lastName.setText(sharedPreferences.getString(LAST_NAME_PREFERENCE_KEY, null))
        email.setText(sharedPreferences.getString(EMAIL_PREFERENCE_KEY, null))
        phoneNumber.setText(sharedPreferences.getString(PHONE_NUMBER_PREFERENCE_KEY, null))
        gender.check(parseGenderFromSharedPreferences())
        language.setText(sharedPreferences.getString(LANGUAGE_PREFERENCE_KEY, null))
        birthdayText.text = sharedPreferences.getString(BIRTHDAY_PREFERENCE_KEY, null)

        ButtonUtils.setUpPopulateButton(
            view,
            R.id.first_name_button, firstName, this.sharedPreferences.getString(FIRST_NAME_PREFERENCE_KEY, SAMPLE_FIRST_NAME)
        )
        ButtonUtils.setUpPopulateButton(
            view,
            R.id.last_name_button, lastName, this.sharedPreferences.getString(LAST_NAME_PREFERENCE_KEY, SAMPLE_LAST_NAME)
        )
        ButtonUtils.setUpPopulateButton(
            view,
            R.id.email_button, email, this.sharedPreferences.getString(EMAIL_PREFERENCE_KEY, SAMPLE_EMAIL)
        )
        ButtonUtils.setUpPopulateButton(
            view,
            R.id.language_button, language, this.sharedPreferences.getString(LANGUAGE_PREFERENCE_KEY, SAMPLE_LANGUAGE)
        )
        ButtonUtils.setUpPopulateButton(
            view,
            R.id.phone_number_button,
            phoneNumber,
            this.sharedPreferences.getString(PHONE_NUMBER_PREFERENCE_KEY, SAMPLE_PHONE_NUMBER)
        )

        val populateButton = view.findViewById<Button>(R.id.user_dialog_button_populate)
        val clearButton = view.findViewById<Button>(R.id.user_dialog_button_clear)
        val birthdayButton = view.findViewById<Button>(R.id.birthday_button)

        populateButton.setOnClickListener(this)
        clearButton.setOnClickListener(this)
        birthdayButton.setOnClickListener(this)

        datePickerDialog = DatePickerDialog(
            requireContext(), { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                birthYear = year
                birthMonth = monthOfYear
                birthDay = dayOfMonth
                birthdayText.text = birthdayDisplayValue
                isBirthdaySet = true
            }, calendar[Calendar.YEAR], calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
    }

    override fun onClick(view: View) {
        val id = view.id
        when (id) {
            R.id.user_dialog_button_clear -> {
                clear()
            }

            R.id.user_dialog_button_populate -> {
                populate()
            }

            R.id.birthday_button -> {
                datePickerDialog.show()
            }
        }
    }

    private fun clear() {
        firstName.text.clear()
        lastName.text.clear()
        email.text.clear()
        gender.check(R.id.unspecified)
        birthdayText.text = ""
        isBirthdaySet = false
    }

    private fun populate() {
        if (firstName.text.isEmpty()) {
            firstName.setText(sharedPreferences.getString(FIRST_NAME_PREFERENCE_KEY, SAMPLE_FIRST_NAME))
        }
        if (lastName.text.isEmpty()) {
            lastName.setText(sharedPreferences.getString(LAST_NAME_PREFERENCE_KEY, SAMPLE_LAST_NAME))
        }
        if (language.text.isEmpty()) {
            language.setText(sharedPreferences.getString(LANGUAGE_PREFERENCE_KEY, SAMPLE_LANGUAGE))
        }
        if (email.text.isEmpty()) {
            email.setText(sharedPreferences.getString(EMAIL_PREFERENCE_KEY, SAMPLE_EMAIL))
        }
        if (gender.checkedRadioButtonId == R.id.unspecified) {
            gender.check(SAMPLE_GENDER)
        }
        if (birthdayText.text.isEmpty()) {
            birthdayText.text = sharedPreferences.getString(BIRTHDAY_PREFERENCE_KEY, SAMPLE_BIRTHDAY)
            isBirthdaySet = true
        }
    }

    override fun onExitButtonPressed(isPositive: Boolean) {
        val firstName = firstName.text.toString()
        val lastName = lastName.text.toString()
        val email = email.text.toString()
        val genderResourceId = gender.checkedRadioButtonId
        val genderRadioButton = gender.findViewById<View>(genderResourceId)
        val genderId = gender.indexOfChild(genderRadioButton)
        val language = language.text.toString()
        val phoneNumber = phoneNumber.text.toString()

        getInstance(requireContext()).getCurrentUser { brazeUser ->
            val editor = sharedPreferences.edit()
            if (firstName.isNotBlank()) {
                brazeUser.setFirstName(firstName)
                editor.putString(FIRST_NAME_PREFERENCE_KEY, firstName)
            }
            if (lastName.isNotBlank()) {
                brazeUser.setLastName(lastName)
                editor.putString(LAST_NAME_PREFERENCE_KEY, lastName)
            }
            if (language.isNotBlank()) {
                brazeUser.setLanguage(language)
                editor.putString(LANGUAGE_PREFERENCE_KEY, language)
            }
            if (email.isNotBlank()) {
                editor.putString(EMAIL_PREFERENCE_KEY, email)
                brazeUser.setEmail(email)
            }
            if (phoneNumber.isNotBlank()) {
                editor.putString(PHONE_NUMBER_PREFERENCE_KEY, phoneNumber)
                brazeUser.setPhoneNumber(phoneNumber)
            }
            if (isBirthdaySet) {
                editor.putString(BIRTHDAY_PREFERENCE_KEY, birthdayDisplayValue)
                val month = getMonth(birthMonth)
                if (month != null) {
                    brazeUser.setDateOfBirth(birthYear, month, birthDay)
                }
            }

            saveGenderToPrefs(genderId, brazeUser, editor)
            editor.apply()
        }

        // Flushing manually is not recommended in almost all production situations as
        // Braze automatically flushes data to its servers periodically. This call
        // is solely for testing purposes.
        if (isPositive) {
            getInstance(requireContext()).requestImmediateDataFlush()
        }
        this.dismiss()
    }

    private fun saveGenderToPrefs(genderId: Int, brazeUser: BrazeUser, editor: SharedPreferences.Editor) {
        when (genderId) {
            GENDER_MALE_INDEX -> {
                brazeUser.setGender(Gender.MALE)
                editor.putInt(GENDER_PREFERENCE_KEY, genderId)
            }

            GENDER_FEMALE_INDEX -> {
                brazeUser.setGender(Gender.FEMALE)
                editor.putInt(GENDER_PREFERENCE_KEY, genderId)
            }

            GENDER_OTHER_INDEX -> {
                brazeUser.setGender(Gender.OTHER)
                editor.putInt(GENDER_PREFERENCE_KEY, genderId)
            }

            GENDER_UNKNOWN_INDEX -> {
                brazeUser.setGender(Gender.UNKNOWN)
                editor.putInt(GENDER_PREFERENCE_KEY, genderId)
            }

            GENDER_NOT_APPLICABLE_INDEX -> {
                brazeUser.setGender(Gender.NOT_APPLICABLE)
                editor.putInt(GENDER_PREFERENCE_KEY, genderId)
            }

            GENDER_PREFER_NOT_TO_SAY_INDEX -> {
                brazeUser.setGender(Gender.PREFER_NOT_TO_SAY)
                editor.putInt(GENDER_PREFERENCE_KEY, genderId)
            }

            else -> Log.w(TAG, "Error parsing gender from user preferences.")
        }
    }

    private fun parseGenderFromSharedPreferences(): Int {
        return when (sharedPreferences.getInt(GENDER_PREFERENCE_KEY, GENDER_UNSPECIFIED_INDEX)) {
            GENDER_UNSPECIFIED_INDEX -> R.id.unspecified
            GENDER_MALE_INDEX -> R.id.male
            GENDER_FEMALE_INDEX -> R.id.female
            GENDER_OTHER_INDEX -> R.id.other
            GENDER_UNKNOWN_INDEX -> R.id.unknown
            GENDER_NOT_APPLICABLE_INDEX -> R.id.not_applicable
            GENDER_PREFER_NOT_TO_SAY_INDEX -> R.id.prefer_not_to_say
            else -> {
                Log.w(TAG, "Error parsing gender from shared preferences.")
                R.id.unspecified
            }
        }
    }

    companion object {
        private val TAG = getBrazeLogTag(UserProfileDialog::class.java)
        private const val GENDER_UNSPECIFIED_INDEX = 0
        private const val GENDER_MALE_INDEX = 1
        private const val GENDER_FEMALE_INDEX = 2
        private const val GENDER_OTHER_INDEX = 3
        private const val GENDER_UNKNOWN_INDEX = 4
        private const val GENDER_NOT_APPLICABLE_INDEX = 5
        private const val GENDER_PREFER_NOT_TO_SAY_INDEX = 6

        private val calendar: Calendar = Calendar.getInstance()

        private const val FIRST_NAME_PREFERENCE_KEY = "user.firstname"
        private const val LAST_NAME_PREFERENCE_KEY = "user.lastname"
        private const val LANGUAGE_PREFERENCE_KEY = "user.language"
        private const val EMAIL_PREFERENCE_KEY = "user.email"
        private const val GENDER_PREFERENCE_KEY = "user.gender_resource_id"
        private const val BIRTHDAY_PREFERENCE_KEY = "user.birthday"
        private const val PHONE_NUMBER_PREFERENCE_KEY = "user.phone_number"

        private const val SAMPLE_FIRST_NAME = "Jane"
        private const val SAMPLE_LAST_NAME = "Doe"
        private const val SAMPLE_LANGUAGE = "hi"
        private const val SAMPLE_EMAIL = "janet@braze.com"
        private const val SAMPLE_PHONE_NUMBER = "(212) 555-2733"
        private val SAMPLE_GENDER = R.id.female
        private val SAMPLE_BIRTHDAY = (calendar[Calendar.MONTH] + 1).toString() + "/" + calendar[Calendar.DAY_OF_MONTH] + "/" + calendar[Calendar.YEAR]
    }
}
