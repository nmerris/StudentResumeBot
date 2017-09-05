package com.nmerris.roboresumedb;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value="session")
public class CurrPerson implements Serializable {

    private long personId;

    private boolean dummyDataHasBeenLoaded;

    public CurrPerson() {
        dummyDataHasBeenLoaded = false;
    }

    public boolean isDummyDataHasBeenLoaded() {
        return dummyDataHasBeenLoaded;
    }

    public void setDummyDataHasBeenLoaded(boolean dummyDataHasBeenLoaded) {
        this.dummyDataHasBeenLoaded = dummyDataHasBeenLoaded;
    }

    public long getPersonId() {
        return personId;
    }

    public void setPersonId(long personId) {
        this.personId = personId;
    }
}
