package cash.xcl.server;

import cash.xcl.api.AllMessages;
import cash.xcl.api.AllMessagesLookup;
import cash.xcl.api.dto.*;
import cash.xcl.api.util.AbstractAllMessages;
import cash.xcl.api.util.CountryRegion;
import cash.xcl.api.util.XCLBase32;

/**
 * This accepts message from the XCLServer and passes them to the appropriate downstream component
 */
public class VanillaGateway extends AbstractAllMessages implements Gateway {
    private final long regionAddress;
    private final String region;
    private final BlockEngine main;
    private final BlockEngine local;

    public VanillaGateway(long address, long regionAddress, String region, BlockEngine main, BlockEngine local) {
        super(address);
        this.regionAddress = regionAddress;
        this.region = region;
        this.main = main;
        this.local = local;
    }

    public static VanillaGateway newGateway(long address, String region, long[] clusterAddresses) {
        long regionAddress = XCLBase32.decode(region);
        String region2 = XCLBase32.encode(regionAddress);
        return new VanillaGateway(address,
                regionAddress,
                region2,
                BlockEngine.newMain(address, 1000, clusterAddresses),
                BlockEngine.newLocal(address, region2, 100, clusterAddresses)
        );
    }

    @Override
    public void allMessagesLookup(AllMessagesLookup lookup) {
        super.allMessagesLookup(lookup);
        main.allMessagesLookup(lookup);
        local.allMessagesLookup(lookup);
    }

    @Override
    public AllMessages to(long addressOrRegion) {
        if (addressOrRegion == 0)
            return main;
        if (addressOrRegion == regionAddress)
            return local;
        return super.to(addressOrRegion);
    }

    @Override
    public void transactionBlockEvent(TransactionBlockEvent transactionBlockEvent) {
        String region = transactionBlockEvent.region();
        if (CountryRegion.MAIN_NAME.equals(region))
            main.transactionBlockEvent(transactionBlockEvent);
        else if (this.region.equals(region))
            local.transactionBlockEvent(transactionBlockEvent);
        else
            System.err.println("Unknown region " + region);
    }

    @Override
    public void transactionBlockGossipEvent(TransactionBlockGossipEvent transactionBlockGossipEvent) {
        String region = transactionBlockGossipEvent.region();
        if (CountryRegion.MAIN_NAME.equals(region))
            main.transactionBlockGossipEvent(transactionBlockGossipEvent);
        else if (this.region.equals(region))
            local.transactionBlockGossipEvent(transactionBlockGossipEvent);
        else
            System.err.println("Unknown region " + region);
    }

    @Override
    public void treeBlockEvent(TreeBlockEvent treeBlockEvent) {
        String region = treeBlockEvent.region();
        if (CountryRegion.MAIN_NAME.equals(region))
            main.treeBlockEvent(treeBlockEvent);
        else if (this.region.equals(region))
            local.treeBlockEvent(treeBlockEvent);
        else
            System.err.println("Unknown region " + region);
    }

    @Override
    public void createNewAddressCommand(CreateNewAddressCommand createNewAddressCommand) {
        main.createNewAddressCommand(createNewAddressCommand);
    }

    @Override
    public void createNewAddressEvent(CreateNewAddressEvent createNewAddressEvent) {
        // cannot be received here.
        throw new UnsupportedOperationException();
    }

    @Override
    public void subscriptionQuery(SubscriptionQuery subscriptionQuery) {
        // handled by the caller
    }

    @Override
    public void start() {
        main.start();
        local.start();
    }

    @Override
    public void close() {
        main.close();
        local.close();
    }
}
