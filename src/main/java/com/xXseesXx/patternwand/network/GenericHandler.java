package com.xXseesXx.patternwand.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Handler to redirect message processing.
 */
public abstract class GenericHandler<REQ extends IMessage> implements IMessageHandler<REQ, IMessage> {

    @Override
    public IMessage onMessage(REQ message, MessageContext ctx) {
        processMessage(message, ctx);
        return null;
    }

    public abstract void processMessage(REQ message, MessageContext context);
}
