
package com.apollocurrency.aplwallet.apl.util.env;

/**
 *
 * @author alukin@gmail.com
 */
public enum PosixExitCodes {

  OK(0),
  EX_GENERAL(1),
  EX_SHELL_BUILTIN_MISSUSE(2),
  EX_USAGE(64),
  EX_DATAERR(65),
  EX_NOINPUT(66),
  EX_NOUSER(67),
  EX_NOHOST(68),
  EX_UNAVAILABLE(69),
  EX_SOFTWARE(70),
  EX_OSERR(71),
  EX_OSFILE(72),
  EX_CANTCREAT(73),
  EX_IOERR(74),
  EX_TEMPFAIL(75),
  EX_PROTOCOL(76),
  EX_NOPERM(77),
  EX_CONFIG(78),
  EX_CANNOT_EXEC_CMD(126),
  EX_CMD_NOT_FOUND(127),
  EX_INVALID_ARG_TO_EXIT(128),
  EX_SIG_HUP(129),
  EX_SIG_INT(130),
  EX_SIG_QUIT(131),
  EX_SIG_ILL(132),
  EX_SIG_TRAP(133),
  EX_SIG_ABRT(134),
  EX_SIG_7(135),
  EX_SIG_FPE(136),
  EX_SIG_KILL(137),
  EX_SIG_10(138),
  EX_SIG_11(139),
  EX_SIG_12(140),
  EX_SIG_PIPE(141),
  EX_SIG_ALRM(142),
  EX_SIG_TERM(143),
  EX_SIG_16(144),
  EX_SIG_17(145),
  EX_SIG_18(146),
  EX_SIG_19(147),
  EX_SIG_20(148),
  EX_SIG_21(149),
  EX_SIG_22(150),
  EX_SIG_23(151),
  EX_SIG_24(152),
  EX_SIG_25(153),
  EX_SIG_26(154),
  EX_SIG_27(155),
  EX_SIG_28(156),
  EX_SIG_29(157),
  EX_SIG_30(158),
  EX_SIG_31(159),

  EX_STATUS_OUT_OF_RANGE(255),
  EX_UNKNOWN(-1);

  private final int exitCode;

  PosixExitCodes(final int code) {
    this.exitCode = code;
  }

  public int exitCode() {
    return exitCode;
  }


}

