package com.github.xpwu.ktdbtable_processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class Logger {

  public Logger(Messager messager) {
    this.messager_ = messager;
  }

  public void error(Element e, String msg, Object... args) {
    messager_.printMessage(
      Diagnostic.Kind.ERROR
      , String.format(msg, args)
      , e);
  }

  public void log(Element e, String msg, Object... args) {
    messager_.printMessage(
      Diagnostic.Kind.NOTE
      , String.format(msg, args)
      , e);
  }

  public void warn(Element e, String msg, Object... args) {
    messager_.printMessage(
      Diagnostic.Kind.WARNING
      , String.format(msg, args)
      , e);
  }

  private Messager messager_;
}
