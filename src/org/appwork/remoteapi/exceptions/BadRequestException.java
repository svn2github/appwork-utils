package org.appwork.remoteapi.exceptions;

import org.appwork.remoteapi.exceptions.RemoteAPIError;
import org.appwork.remoteapi.exceptions.RemoteAPIException;

public class BadRequestException extends RemoteAPIException {

    public BadRequestException(String details) {
        super(RemoteAPIError.BAD_PARAMETERS);

    }

}
