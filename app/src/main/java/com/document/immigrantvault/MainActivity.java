package com.document.immigrantvault;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.document.immigrantvault.databinding.ActivityMainBinding;
import com.document.immigrantvault.ui.lock.LockActivity;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static long lastPausedAt = 0;
    private static final long RELOCK_TIMEOUT_MS = 30_000;

    private ActivityMainBinding binding;
    private NavController navController;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermissionIfNeeded();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        Set<Integer> topLevel = new HashSet<>();
        topLevel.add(R.id.homeFragment);
        topLevel.add(R.id.remindersFragment);
        topLevel.add(R.id.settingsFragment);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(topLevel).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                navController.navigate(R.id.homeFragment);
            } else if (id == R.id.nav_reminders) {
                navController.navigate(R.id.remindersFragment);
            } else if (id == R.id.nav_settings) {
                navController.navigate(R.id.settingsFragment);
            }
            return true;
        });

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) getApplication();
        app.getPersonRepository().ensureSelfExists();
    }

    @Override
    protected void onPause() {
        super.onPause();
        lastPausedAt = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lastPausedAt > 0 && System.currentTimeMillis() - lastPausedAt > RELOCK_TIMEOUT_MS) {
            startActivity(new Intent(this, LockActivity.class));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
