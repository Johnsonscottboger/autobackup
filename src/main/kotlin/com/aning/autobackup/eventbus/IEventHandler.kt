package com.aning.autobackup.eventbus

interface IEventHandler<in TEvent : IEvent> {
    fun handle(e: TEvent)
}