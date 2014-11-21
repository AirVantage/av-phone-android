package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;

public interface SyncWithAvListener {
    public void onSynced(AvError error);
}
