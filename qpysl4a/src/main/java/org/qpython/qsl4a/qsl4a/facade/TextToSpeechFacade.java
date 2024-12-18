/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.qpython.qsl4a.qsl4a.facade;

import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.util.concurrent.CountDownLatch;

/**
 * Provides Text To Speech services for API 4 or more.
 */

public class TextToSpeechFacade extends RpcReceiver {

  private final TextToSpeech mTts;
  private final CountDownLatch mOnInitLock;

  public TextToSpeechFacade(FacadeManager manager) {
    super(manager);
    mOnInitLock = new CountDownLatch(1);
    mTts = new TextToSpeech(manager.getService(), new OnInitListener() {
      @Override
      public void onInit(int arg0) {
        mOnInitLock.countDown();
      }
    });
  }

  @Override
  public void shutdown() {
    while (mTts.isSpeaking()) {
      SystemClock.sleep(100);
    }
    mTts.shutdown();
  }

  @Rpc(description = "Speaks the provided message via TTS.")
  public void ttsSpeak(
          @RpcParameter(name = "message") String message,
          @RpcParameter(name = "pitch") @RpcDefault("1.0") Double pitch,
          @RpcParameter(name = "pitchRate") @RpcDefault("1.0") Double pitchRate
  ) throws InterruptedException {
    mOnInitLock.await();
    if (message != null) {
      mTts.setSpeechRate(pitchRate.floatValue());
      mTts.setPitch(pitch.floatValue());
      mTts.speak(message, TextToSpeech.QUEUE_ADD, null,null);
    }
  }

  @Rpc(description = "Returns True if speech is currently in progress.")
  public Boolean ttsIsSpeaking() throws InterruptedException {
    mOnInitLock.await();
    return mTts.isSpeaking();
  }

}
