package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import net.airvantage.model.AvSystem;

public class SyncWithAvResult {

    private AvError error;
    
    private AvSystem system;
    
    public SyncWithAvResult(AvError error) {
        this.error = error;
    }
    
    public SyncWithAvResult(AvSystem system) {
        this.system = system;
    }
    
    public boolean isError() {
        return error != null;
    }
    
    public AvSystem getSystem() {
        return system;
    }
    
    public AvError getError() {
        return error;
    }
}
