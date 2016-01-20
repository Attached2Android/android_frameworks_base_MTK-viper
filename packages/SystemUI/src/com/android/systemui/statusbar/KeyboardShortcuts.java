/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.statusbar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.KeyboardShortcutInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager.KeyboardShortcutsReceiver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.recents.Recents;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.graphics.Color.WHITE;
import static android.view.Gravity.TOP;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;

/**
 * Contains functionality for handling keyboard shortcuts.
 */
public class KeyboardShortcuts {
    private static final char SYSTEM_HOME_BASE_CHARACTER = '\u2386';
    private static final char SYSTEM_BACK_BASE_CHARACTER = '\u007F';
    private static final char SYSTEM_RECENTS_BASE_CHARACTER = '\u0009';

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;
    private final OnClickListener dialogCloseListener =  new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            dismissKeyboardShortcutsDialog();
        }
    };

    private Dialog mKeyboardShortcutsDialog;

    public KeyboardShortcuts(Context context) {
        this.mContext = context;
    }

    public void toggleKeyboardShortcuts() {
        if (mKeyboardShortcutsDialog == null) {
            Recents.getSystemServices().requestKeyboardShortcuts(mContext,
                new KeyboardShortcutsReceiver() {
                    @Override
                    public void onKeyboardShortcutsReceived(
                            final List<KeyboardShortcutGroup> result) {
                        KeyboardShortcutGroup systemGroup = new KeyboardShortcutGroup(
                            mContext.getString(R.string.keyboard_shortcut_group_system), true);
                        systemGroup.addItem(new KeyboardShortcutInfo(
                            mContext.getString(R.string.keyboard_shortcut_group_system_home),
                            SYSTEM_HOME_BASE_CHARACTER, KeyEvent.META_META_ON));
                        systemGroup.addItem(new KeyboardShortcutInfo(
                            mContext.getString(R.string.keyboard_shortcut_group_system_back),
                            SYSTEM_BACK_BASE_CHARACTER, KeyEvent.META_META_ON));
                        systemGroup.addItem(new KeyboardShortcutInfo(
                            mContext.getString(R.string.keyboard_shortcut_group_system_recents),
                            SYSTEM_RECENTS_BASE_CHARACTER, KeyEvent.META_ALT_ON));
                        result.add(systemGroup);
                        showKeyboardShortcutsDialog(result);
                    }
                });
        } else {
            dismissKeyboardShortcutsDialog();
        }
    }

    public void dismissKeyboardShortcutsDialog() {
        if (mKeyboardShortcutsDialog != null) {
            mKeyboardShortcutsDialog.dismiss();
            mKeyboardShortcutsDialog = null;
        }
    }

    private void showKeyboardShortcutsDialog(
            final List<KeyboardShortcutGroup> keyboardShortcutGroups) {
        // Need to post on the main thread.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: break all this code out into a handleShowKeyboard...
                // Might add more things posted; should consider adding a custom handler so
                // you can send the keyboardShortcutsGroups as part of the message.
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        LAYOUT_INFLATER_SERVICE);
                final View keyboardShortcutsView = inflater.inflate(
                        R.layout.keyboard_shortcuts_view, null);
                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                ScrollView scrollView = (ScrollView) keyboardShortcutsView.findViewById(
                        R.id.keyboard_shortcuts_scroll_view);
                // TODO: find a better way to set the height.
                scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        (int) (dm.heightPixels * dm.density)));

                populateKeyboardShortcuts((LinearLayout) keyboardShortcutsView.findViewById(
                        R.id.keyboard_shortcuts_container), keyboardShortcutGroups);
                dialogBuilder.setView(keyboardShortcutsView);
                dialogBuilder.setPositiveButton(R.string.quick_settings_done, dialogCloseListener);
                mKeyboardShortcutsDialog = dialogBuilder.create();
                mKeyboardShortcutsDialog.setCanceledOnTouchOutside(true);

                // Setup window.
                Window keyboardShortcutsWindow = mKeyboardShortcutsDialog.getWindow();
                keyboardShortcutsWindow.setType(TYPE_SYSTEM_DIALOG);
                keyboardShortcutsWindow.setBackgroundDrawable(
                        mContext.getDrawable(R.color.ksh_dialog_background_color));
                keyboardShortcutsWindow.setGravity(TOP);
                mKeyboardShortcutsDialog.show();
            }
        });
    }

    private void populateKeyboardShortcuts(LinearLayout keyboardShortcutsLayout,
            List<KeyboardShortcutGroup> keyboardShortcutGroups) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final int keyboardShortcutGroupsSize = keyboardShortcutGroups.size();
        for (int i = 0; i < keyboardShortcutGroupsSize; i++) {
            KeyboardShortcutGroup group = keyboardShortcutGroups.get(i);
            TextView categoryTitle = (TextView) inflater.inflate(
                    R.layout.keyboard_shortcuts_category_title, keyboardShortcutsLayout, false);
            categoryTitle.setText(group.getLabel());
            categoryTitle.setTextColor(group.isSystemGroup()
                    ? mContext.getColor(R.color.ksh_system_group_color)
                    : mContext.getColor(R.color.ksh_application_group_color));
            keyboardShortcutsLayout.addView(categoryTitle);

            LinearLayout shortcutWrapper = (LinearLayout) inflater.inflate(
                    R.layout.keyboard_shortcuts_wrapper, null);
            final int itemsSize = group.getItems().size();
            for (int j = 0; j < itemsSize; j++) {
                KeyboardShortcutInfo info = group.getItems().get(j);
                View shortcutView = inflater.inflate(R.layout.keyboard_shortcut_app_item, null);
                TextView textView = (TextView) shortcutView
                        .findViewById(R.id.keyboard_shortcuts_keyword);
                textView.setText(info.getLabel());

                List<String> shortcutKeys = getHumanReadableShortcutKeys(info);
                final int shortcutKeysSize = shortcutKeys.size();
                for (int k = 0; k < shortcutKeysSize; k++) {
                    String shortcutKey = shortcutKeys.get(k);
                    TextView shortcutKeyView = (TextView) inflater.inflate(
                            R.layout.keyboard_shortcuts_key_view, null);
                    shortcutKeyView.setText(shortcutKey);
                    LinearLayout shortcutItemsContainer = (LinearLayout) shortcutView
                            .findViewById(R.id.keyboard_shortcuts_item_container);
                    shortcutItemsContainer.addView(shortcutKeyView);
                }
                shortcutWrapper.addView(shortcutView);
            }

            // TODO: merge container and wrapper into one xml file - wrapper is always a child of
            // container.
            LinearLayout shortcutsContainer = (LinearLayout) inflater.inflate(
                    R.layout.keyboard_shortcuts_container, null);
            shortcutsContainer.addView(shortcutWrapper);
            keyboardShortcutsLayout.addView(shortcutsContainer);
        }
    }

    private List<String> getHumanReadableShortcutKeys(KeyboardShortcutInfo info) {
        // TODO: fix the shortcuts. Find or build an util which can produce human readable
        // names of the baseCharacter and the modifiers.
        List<String> shortcutKeys = new ArrayList<>();
        shortcutKeys.add(KeyEvent.metaStateToString(info.getModifiers()).toUpperCase());
        shortcutKeys.add(Character.getName(info.getBaseCharacter()).toUpperCase());
        return shortcutKeys;
    }
}
