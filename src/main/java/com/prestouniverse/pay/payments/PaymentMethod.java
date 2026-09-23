package com.prestouniverse.pay.payments;

/**
 * Known payment method codes for {@link PaymentInitRequest.Builder#allowedPaymentMethods(String...)} and {@link
 * PaymentDetail#method()}. The gateway may return values not listed here.
 */
public final class PaymentMethod {

    public static final String Wallet = "Wallet";
    public static final String CashBack = "CashBack";
    public static final String Card = "Card";
    public static final String BigLife = "BigLife";
    public static final String BonusLink = "BonusLink";
    public static final String RISE = "RISE";
    public static final String PlusMiles = "PlusMiles";
    public static final String VSing = "VSing";
    public static final String KLEAN = "KLEAN";
    public static final String GOrewards = "GOrewards";
    public static final String Subwallet_NearU = "Subwallet_NearU";
    public static final String Subwallet_CARROTS = "Subwallet_CARROTS";
    public static final String Subwallet_BUDDY = "Subwallet_BUDDY";
    public static final String TuneTalk = "TuneTalk";
    public static final String PmPgCard = "PmPgCard";
    public static final String Maybank = "Maybank";
    public static final String Ambank = "Ambank";
    public static final String Rhb = "Rhb";
    public static final String HongLeong = "HongLeong";
    public static final String Cimb = "Cimb";
    public static final String PublicBank = "PublicBank";
    public static final String AffinBank = "AffinBank";
    public static final String Bsn = "Bsn";
    public static final String AllianceBank = "AllianceBank";
    public static final String AgroBank = "AgroBank";
    public static final String BankIslam = "BankIslam";
    public static final String BankOfChina = "BankOfChina";
    public static final String BankRakyat = "BankRakyat";
    public static final String BankMuamalat = "BankMuamalat";
    public static final String BoostBank = "BoostBank";
    public static final String HsbcBank = "HsbcBank";
    public static final String KuwaitFinanceHouse = "KuwaitFinanceHouse";
    public static final String OcbcBank = "OcbcBank";
    public static final String AlRajhiBank = "AlRajhiBank";
    public static final String StandardChartered = "StandardChartered";
    public static final String UobBank = "UobBank";
    public static final String MbsbBank = "MbsbBank";
    public static final String HongLeongPex = "HongLeongPex";
    public static final String UnionPay = "UnionPay";
    public static final String UnionPayQR = "UnionPayQR";
    public static final String Boost = "Boost";
    public static final String GrabPay = "GrabPay";
    public static final String GrabPayLater = "GrabPayLater";
    public static final String WeChatPayChina = "WeChatPayChina";
    public static final String TouchNGo = "TouchNGo";
    public static final String TouchNGoEWallet = "TouchNGoEWallet";
    public static final String AliPayChina = "AliPayChina";
    public static final String LatitudePay = "LatitudePay";

    private PaymentMethod() {
    }
}
