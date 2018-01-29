package net.airvantage.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.sierrawireless.avphone.R;

public class UserRights extends ArrayList<String> {

    private static final long serialVersionUID = 1L;

    private static Map<String, Integer> AS_STRING_ID = new HashMap<>();

    static {
        AS_STRING_ID.put("entities.systems.view", R.string.entities_systems_view);
        AS_STRING_ID.put("entities.systems.create", R.string.entities_systems_create);
        AS_STRING_ID.put("entities.systems.edit", R.string.entities_systems_edit);
        AS_STRING_ID.put("entities.applications.view", R.string.entities_applications_view);
        AS_STRING_ID.put("entities.applications.create", R.string.entities_applications_create);
        AS_STRING_ID.put("entities.applications.edit", R.string.entities_applications_edit);
        AS_STRING_ID.put("entities.alerts.rule.view", R.string.entities_alerts_rule_view);
        AS_STRING_ID.put("entities.alerts.rule.create.edit.delete", R.string.entities_alerts_rule_create_edit_delete);
    }

    public static String asString(String rightKey, Context context) {
        String res = null;
        Integer stringId = AS_STRING_ID.get(rightKey);
        if (stringId != null) {
            res = context.getString(stringId);
        }
        return res;
    }

}
