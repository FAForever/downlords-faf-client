package com.faforever.client.legacy.update;

import com.faforever.client.legacy.writer.QDataInputStream;
import com.faforever.client.legacy.relay.RelayServerCommandTypeAdapter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Collections;

class UpdateServerReader implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final InputStream inputStream;
  private UpdateServerResponseListener updateServerResponseListener;
  private final Gson gson;

  private boolean stopped;

  public UpdateServerReader(InputStream inputStream, UpdateServerResponseListener updateServerResponseListener) {
    this.inputStream = inputStream;
    this.updateServerResponseListener = updateServerResponseListener;

    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(UpdateServerCommand.class, new RelayServerCommandTypeAdapter())
        .create();
  }

  public void blockingRead() throws IOException {
    try (QDataInputStream dataInput = new QDataInputStream(new DataInputStream(new BufferedInputStream(inputStream)))) {
      while (!stopped) {
        dataInput.skipBlockSize();
        String action = dataInput.readQString();

        UpdateServerCommand updateServerCommand = UpdateServerCommand.fromString(action);
        dispatchServerCommand(updateServerCommand, dataInput);
      }
    } catch (EOFException e) {
      logger.info("Disconnected from FAF update server (EOF)");
    }
  }

  private void dispatchServerCommand(UpdateServerCommand command, QDataInputStream dataInput) throws IOException {
    switch (command) {
      case PATH_TO_SIM_MOD:
        updateServerResponseListener.onSimModPath(dataInput.readQString());
        break;

      case SIM_MOD_NOT_FOUND:
        updateServerResponseListener.onSimModNotFound();
        break;

      case LIST_FILES_TO_UP:
        String files = dataInput.readQString();
        // FIXME parse files
        updateServerResponseListener.onFilesToUpdate(Collections.emptySet());
        break;

      case UNKNOWN_APP:
        updateServerResponseListener.onUnknownApp();
        break;

      case THIS_PATCH_IS_IN_CREATION_EXCEPTION:
        updateServerResponseListener.onServerBusy();
        break;

      case VERSION_PATCH_NOT_FOUND:
        updateServerResponseListener.onVersionPatchNotFound(dataInput.readQString());
        break;

      case VERSION_MOD_PATCH_NOT_FOUND:
        updateServerResponseListener.onModPatchNotFound(dataInput.readQString());
        break;

      case PATCH_NOT_FOUND:
        updateServerResponseListener.onPatchNotFound(dataInput.readQString());
        break;

      case UP_TO_DATE:
        updateServerResponseListener.onFileUpToDate(dataInput.readQString());
        break;

      case ERROR_FILE:
        updateServerResponseListener.onFileNotFound(dataInput.readQString());
        break;

      case SEND_FILE_PATH:
        String path = dataInput.readQString();
        String fileToCopy = dataInput.readQString();
        String url = dataInput.readQString();
        updateServerResponseListener.onSendFilePath(path, fileToCopy, url);
        break;

      case SEND_FILE:
        updateServerResponseListener.onSendFile(dataInput.readQString());
        break;

      default:
        throw new IllegalStateException("Unhandled update server command: " + command);
    }
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }
}
