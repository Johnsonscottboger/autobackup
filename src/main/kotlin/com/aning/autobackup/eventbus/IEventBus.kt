package com.aning.autobackup.eventbus

interface IEventBus {

    fun <TEvent : IEvent, TEventHandler : IEventHandler<TEvent>> subscribe(eventHandlerType: Class<TEventHandler>)

    fun <TEvent : IEvent, TEventHandler : IEventHandler<TEvent>> unsubscribe(eventHandlerType: Class<TEventHandler>)

    fun <TEvent : IEvent> publish(event: TEvent)

    fun <TEvent : IEvent> publishAsync(event: TEvent)
}