package org.appwork.shutdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasicShutdownRequest implements ShutdownRequest {

    private boolean                          silent;
    private ArrayList<ShutdownVetoException> vetos;

    /**
     * @param b
     */
    public BasicShutdownRequest(final boolean silent) {
        this.silent = silent;
        vetos = new ArrayList<ShutdownVetoException>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.shutdown.ShutdownRequest#onShutdown()
     */
    @Override
    public void onShutdown() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.shutdown.ShutdownRequest#onShutdownVeto()
     */
    @Override
    public void onShutdownVeto() {
        // TODO Auto-generated method stub

    }

    /**
     * 
     */
    public BasicShutdownRequest() {
        this(false);
    }

    @Override
    public boolean askForVeto(final ShutdownVetoListener listener) {
        return true;
    }

    @Override
    public void addVeto(final ShutdownVetoException e) {
        synchronized (vetos) {
            vetos.add(e);

        }

    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public List<ShutdownVetoException> getVetos() {
        synchronized (vetos) {

            return Collections.unmodifiableList(vetos);

        }
    }

    @Override
    public boolean hasVetos() {
        synchronized (vetos) {
            return vetos.size() > 0;
        }
    }

}
