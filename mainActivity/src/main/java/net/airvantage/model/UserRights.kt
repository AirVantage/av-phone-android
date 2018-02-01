package net.airvantage.model

import java.util.ArrayList
import java.util.HashMap

import android.content.Context

import com.sierrawireless.avphone.R

class UserRights : ArrayList<String>() {
    companion object {

        private const val serialVersionUID = 1L

        private val AS_STRING_ID = HashMap<String, Int>()

        init {
            AS_STRING_ID["entities.systems.view"] = R.string.entities_systems_view
            AS_STRING_ID["entities.systems.create"] = R.string.entities_systems_create
            AS_STRING_ID["entities.systems.edit"] = R.string.entities_systems_edit
            AS_STRING_ID["entities.applications.view"] = R.string.entities_applications_view
            AS_STRING_ID["entities.applications.create"] = R.string.entities_applications_create
            AS_STRING_ID["entities.applications.edit"] = R.string.entities_applications_edit
            AS_STRING_ID["entities.alerts.rule.view"] = R.string.entities_alerts_rule_view
            AS_STRING_ID["entities.alerts.rule.create.edit.delete"] = R.string.entities_alerts_rule_create_edit_delete
        }

        fun asString(rightKey: String, context: Context): String? {
            var res: String? = null
            val stringId = AS_STRING_ID[rightKey]
            if (stringId != null) {
                res = context.getString(stringId)
            }
            return res
        }
    }

}
