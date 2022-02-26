package org.ucombinator.jade.util

import mu.KotlinLogging
import mu.KLogger

interface Log {
  val logger: KLogger
}

class Logger : Log {
  override val logger = KotlinLogging.logger {} // TODO: lazy?
}
