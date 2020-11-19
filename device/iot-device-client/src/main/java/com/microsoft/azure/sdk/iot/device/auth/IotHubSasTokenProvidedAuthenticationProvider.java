/*
*  Copyright (c) Microsoft. All rights reserved.
*  Licensed under the MIT license. See LICENSE file in the project root for full license information.
*/

package com.microsoft.azure.sdk.iot.device.auth;

import com.microsoft.azure.sdk.iot.device.SasTokenProvider;

import javax.net.ssl.SSLContext;

public class IotHubSasTokenProvidedAuthenticationProvider extends IotHubSasTokenAuthenticationProvider
{
    SasTokenProvider sasTokenProvider;
    String lastSasToken;

    public IotHubSasTokenProvidedAuthenticationProvider(String hostName, String deviceId, String moduleId, SasTokenProvider sasTokenProvider, SSLContext sslContext) {
        super(hostName, null, deviceId, moduleId, sslContext);

        if (sasTokenProvider == null)
        {
            throw new IllegalArgumentException("sas token provider cannot be null");
        }

        this.sasTokenProvider = sasTokenProvider;
    }

    @Override
    public boolean isRenewalNecessary()
    {
        return false;
    }

    @Override
    public void setTokenValidSecs(long tokenValidSecs)
    {
        throw new UnsupportedOperationException("Cannot configure sas token time to live when custom sas token provider is in use");
    }

    @Override
    public boolean canRefreshToken()
    {
        return true;
    }

    @Override
    public String getRenewedSasToken()
    {
        lastSasToken = sasTokenProvider.getSasToken();
        return lastSasToken;
    }

    @Override
    public int getMillisecondsBeforeProactiveRenewal()
    {
        // Seconds since UNIX epoch when this sas token will expire
        long expiryTimeSeconds = IotHubSasToken.getExpiryTimeFromToken(lastSasToken);

        // Assuming that the token's life "starts" now for the sake of figuring out when it needs to be renewed. Users
        // could theoretically give us a SAS token that started a while ago, but since we have no way of figuring that out,
        // we will conservatively just renew at 85% of the remaining time on the token, rather than 85% of the time the token
        // has existed for.
        long tokenValidSeconds = expiryTimeSeconds - (System.currentTimeMillis() / 1000);

        double timeBufferMultiplier = this.timeBufferPercentage / 100.0; //Convert 85 to .85, for example. Percentage multipliers are in decimal
        return (int) (tokenValidSeconds * 1000 * timeBufferMultiplier);
    }
}