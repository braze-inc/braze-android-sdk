package com.appboy.sample.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentContainerView
import com.appboy.sample.R
import com.appboy.sample.activity.settings.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.settings_page)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.settings)
        toolbar.navigationIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_back_button_droidboy, null)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        applyEdgeToEdgeInsets(toolbar, findViewById(R.id.settingsFragmentContainer))

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settingsFragmentContainer, SettingsFragment())
            .commit()
    }

    private fun applyEdgeToEdgeInsets(
        toolbar: Toolbar,
        fragmentContainer: FragmentContainerView,
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            windowInsets
        }
    }
}
