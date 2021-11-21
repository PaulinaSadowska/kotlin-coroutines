package com.example.android.advancedcoroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@FlowPreview
@ExperimentalCoroutinesApi
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
    @Test
    fun `collect values from flow`() {
        runBlockingTest {
            makeFlow().map { value ->
                println("got $value")
                value
            }
                    .collect()
            println("flow is completed")
        }
    }


    @Test
    fun `flow to list`() {
        runBlockingTest {
            val values = makeFlow().map {
                println("maps $it")
                it
            }.toList()
            values.forEach {
                println("got $it")
            }
            println("flow is completed")
        }
    }


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


    @Test
    fun `flow with flatMapLatest`() {
        runBlockingTest {
            val result = flow {
                emit("a")
                delay(100)
                emit("b")
            }.flatMapLatest { value ->
                flow {
                    emit(value + "_0")
                    delay(200)
                    emit(value + "_1")
                }
            }.toList()
            print(result.joinToString())
        }
        // works the same as switch map
        // a_0, b_0, b_1
    }


    @Test
    fun `flow with flatMapLatest 2`() {
        runBlockingTest {
            val result = flow {
                emit("a")
                delay(200)
                emit("b")
            }.flatMapLatest { value ->
                flow {
                    emit(value + "_0")
                    delay(100)
                    emit(value + "_1")
                }
            }.toList()
            print(result.joinToString())
        }
        // works the same as switch map
        // a_0, a_1, b_0, b_1
    }

    @FlowPreview
    @Test
    fun `flow with flatMapMerge`() {
        runBlockingTest {
            val result = flow {
                emit("a")
                delay(100)
                emit("b")
            }.flatMapMerge { value ->
                flow {
                    emit(value + "_0")
                    delay(200)
                    emit(value + "_1")
                }
            }.toList()
            print(result.joinToString())
        }
        // a_0, b_0, a_1, b_1
    }


    @Test
    fun `flow with flatMapConcat`() {
        runBlockingTest {
            val result = flow {
                emit("a")
                delay(100)
                emit("b")
            }.flatMapConcat { value ->
                flow {
                    emit(value + "_0")
                    delay(200)
                    emit(value + "_1")
                }
            }.toList()
            print(result.joinToString())
        }
        //a_0, a_1, b_0, b_1
    }

    @Test
    fun `flow with flatMapConcat 2`() {
        runBlockingTest {
            val result = flow {
                emit("a")
                delay(200)
                emit("b")
            }.flatMapConcat { value ->
                flow {
                    emit(value + "_0")
                    delay(100)
                    emit(value + "_1")
                }
            }.toList()
            print(result.joinToString())
        }
        //a_0, a_1, b_0, b_1
    }

}