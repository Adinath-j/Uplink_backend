package com.example.Uplink_Backend.modules.qa.dto;

public class DoubtToggleResponse {

    private boolean hasMarked;
    private int count;

    public DoubtToggleResponse() {}

    public DoubtToggleResponse(boolean hasMarked, int count) {
        this.hasMarked = hasMarked;
        this.count = count;
    }

    public boolean isHasMarked() {
        return hasMarked;
    }

    public void setHasMarked(boolean hasMarked) {
        this.hasMarked = hasMarked;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

