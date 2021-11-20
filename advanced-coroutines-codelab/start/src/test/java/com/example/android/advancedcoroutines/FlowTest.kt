package com.example.android.advancedcoroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.collections.forEach

class FlowTest {

    private fun makeFlow() = flow {
        println("sending first value")
        emit(1)
        println("first value collected, sending another value")
        emit(2)
        println("second value collected, sending a third value")
        emit(3)
        println("done")
    }

    @InternalCoroutinesApi
    @ExperimentalCoroutinesApi
    @Test
    fun `collect values from flow`() {
        runBlockingTest {
            makeFlow().map {value ->
                        println("got $value")
                        value
                    }
                    .collect()
            println("flow is completed")
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `flow to list`() {
        runBlockingTest {
            val values = makeFlow().map{
                println("maps $it")
                it
            }.toList()
            values.forEach {
                println("got $it")
            }
            println("flow is completed")
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `flow with take`() {
        runBlockingTest {
            val repeatableFlow = makeFlow().take(2)
            println("collect called (1):")
            repeatableFlow.collect()
            println("collect called (2):")
            repeatableFlow.collect()
            println("flow is completed")
        }
    }
}