package com.prestouniverse.pay.payments;

/**
 * Known payment method codes for {@link PaymentInitRequest.Builder#allowedPaymentMethods(String...)} and {@link
 * PaymentDetail#method()}. The gateway may return values not listed here.
 */
public final class PaymentMethod {

    public static final String WALLET = "Wallet";
    public static final String CASH_BACK = "CashBack";
    public static final String CARD = "Card";
    public static final String BIG_LIFE = "BigLife";
    public static final String BONUS_LINK = "BonusLink";
    public static final String RISE = "RISE";
    public static final String PLUS_MILES = "PlusMiles";
    public static final String V_SING = "VSing";
    public static final String KLEAN = "KLEAN";
    public static final String GO_REWARDS = "GOrewards";
    public static final String SUBWALLET_NEAR_U = "Subwallet_NearU";
    public static final String SUBWALLET_CARROTS = "Subwallet_CARROTS";
    public static final String SUBWALLET_BUDDY = "Subwallet_BUDDY";
    public static final String TUNE_TALK = "TuneTalk";
    public static final String PM_PG_CARD = "PmPgCard";
    public static final String MAYBANK = "Maybank";
    public static final String AMBANK = "Ambank";
    public static final String RHB = "Rhb";
    public static final String HONG_LEONG = "HongLeong";
    public static final String CIMB = "Cimb";
    public static final String PUBLIC_BANK = "PublicBank";
    public static final String AFFIN_BANK = "AffinBank";
    public static final String BSN = "Bsn";
    public static final String ALLIANCE_BANK = "AllianceBank";
    public static final String AGRO_BANK = "AgroBank";
    public static final String BANK_ISLAM = "BankIslam";
    public static final String BANK_OF_CHINA = "BankOfChina";
    public static final String BANK_RAKYAT = "BankRakyat";
    public static final String BANK_MUAMALAT = "BankMuamalat";
    public static final String BOOST_BANK = "BoostBank";
    public static final String HSBC_BANK = "HsbcBank";
    public static final String KUWAIT_FINANCE_HOUSE = "KuwaitFinanceHouse";
    public static final String OCBC_BANK = "OcbcBank";
    public static final String AL_RAJHI_BANK = "AlRajhiBank";
    public static final String STANDARD_CHARTERED = "StandardChartered";
    public static final String UOB_BANK = "UobBank";
    public static final String MBSB_BANK = "MbsbBank";
    public static final String HONG_LEONG_PEX = "HongLeongPex";
    public static final String UNION_PAY = "UnionPay";
    public static final String UNION_PAY_QR = "UnionPayQR";
    public static final String BOOST = "Boost";
    public static final String GRAB_PAY = "GrabPay";
    public static final String GRAB_PAY_LATER = "GrabPayLater";
    public static final String WE_CHAT_PAY_CHINA = "WeChatPayChina";
    public static final String TOUCH_N_GO = "TouchNGo";
    public static final String TOUCH_N_GO_E_WALLET = "TouchNGoEWallet";
    public static final String ALI_PAY_CHINA = "AliPayChina";
    public static final String LATITUDE_PAY = "LatitudePay";

    private PaymentMethod() {
    }
}
