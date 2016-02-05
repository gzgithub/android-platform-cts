; Copyright (C) 2016 The Android Open Source Project
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;      http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

.source ITestDefault2.java
.interface public dot.junit.opcodes.invoke_super.d.ITestDefault2

.method public testDefault()V
.limit regs 2
    return-void
.end method

.source ITestDefault.java
.interface public dot.junit.opcodes.invoke_super.d.ITestDefault

.method public testDefault()V
.limit regs 2
    return-void
.end method

.source ITestConflict.java
.interface public dot.junit.opcodes.invoke_super.d.ITestConflict
.implements dot.junit.opcodes.invoke_super.d.ITestDefault
.implements dot.junit.opcodes.invoke_super.d.ITestDefault2

.source T_invoke_super_28.java
.class public dot.junit.opcodes.invoke_super.d.T_invoke_super_28
.super java/lang/Object
.implements dot.junit.opcodes.invoke_super.d.ITestConflict

.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()V
.limit regs 1
       invoke-super {v0}, dot/junit/opcodes/invoke_super/d/ITestConflict/testDefault()V
       return-void
.end method

