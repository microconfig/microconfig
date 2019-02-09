package deployment.mgmt.atrifacts.strategies.nexus;

import deployment.util.FileLogger;
import lombok.RequiredArgsConstructor;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.eclipse.aether.transfer.TransferEvent.RequestType.PUT;

@RequiredArgsConstructor
public class LoggerTransferListener implements TransferListener {
    private final FileLogger logger;

    @Override
    public void transferInitiated(TransferEvent event) {
        String message = event.getRequestType() == PUT ? "[Transfer] Uploading" : "[Transfer] Downloading";
        logger.info(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
    }

    @Override
    public void transferStarted(TransferEvent transferEvent) {
    }

    @Override
    public void transferProgressed(TransferEvent event) {
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();

        if (contentLength >= 0) {
            String type = (event.getRequestType() == PUT ? "[Transfer] Uploaded" : "[Transfer] Downloaded");
            String len = contentLength >= 1024 ? toKb(contentLength) + " KB" : contentLength + " B";

            String throughput = "";
            long duration = System.currentTimeMillis() - resource.getTransferStartTime();
            if (duration > 0) {
                DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                double kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);
                throughput = " at " + format.format(kbPerSec) + " KB/sec";
            }

            logger.info(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len + throughput + ")");
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        logger.info("[Transfer] Transfer failed: " + event.getResource().getResourceName());
    }

    @Override
    public void transferCorrupted(TransferEvent event) {
        logger.info("[Transfer] Transfer corrupted: " + event.getResource().getResourceName() + event.getException().getMessage());
    }

    private long toKb(long bytes) {
        return (bytes + 1023) / 1024;
    }
}
