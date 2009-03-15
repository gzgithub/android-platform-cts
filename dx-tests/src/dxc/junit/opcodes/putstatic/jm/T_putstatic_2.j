; Copyright (C) 2008 The Android Open Source Project
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

.source T_putstatic_2.java
.class public dxc/junit/opcodes/putstatic/jm/T_putstatic_2
.super java/lang/Object

.field public static st_d1 D

.method public <init>()V
    aload_0
    invokespecial java/lang/Object/<init>()V
    return
.end method

.method public run()V
    .limit stack 2
    ldc2_w 1000000.0
    putstatic dxc.junit.opcodes.putstatic.jm.T_putstatic_2.st_d1 D
    return
.end method
