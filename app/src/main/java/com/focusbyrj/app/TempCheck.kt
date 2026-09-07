package com.focusbyrj.app

import app.rive.runtime.kotlin.controllers.RiveFileController

fun check(c: RiveFileController) {
    val inputs = c.stateMachines.flatMap { it.inputs }.map { it.name }
}
