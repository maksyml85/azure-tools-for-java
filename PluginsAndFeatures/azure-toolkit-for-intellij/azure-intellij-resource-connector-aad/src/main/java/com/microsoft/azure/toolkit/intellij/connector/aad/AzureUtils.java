/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ApplicationAddPasswordParameterSet;
import com.microsoft.graph.models.Domain;
import com.microsoft.graph.models.PasswordCredential;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

final class AzureUtils {
    private static final Logger LOG = Logger.getInstance(AzureUtils.class);

    private AzureUtils() {
    }

    /**
     * Creates a new client password credential for the given application.
     *
     * @param client      The Microsoft Graph client
     * @param application The application to associate with the new credentials.
     * @return The new credentials.
     * @throws com.microsoft.graph.core.ClientException If the credentials could not be created
     */
    @Nonnull
    @AzureOperation("azure/aad.create_client_secret")
    static PasswordCredential createApplicationClientSecret(@Nonnull GraphServiceClient<Request> client,
                                                            @Nonnull Application application) {
        assert application.id != null;

        var credential = new PasswordCredential();
        var secret = ApplicationAddPasswordParameterSet.newBuilder().withPasswordCredential(credential).build();
        return client.applications(application.id)
                .addPassword(secret)
                .buildRequest()
                .post();
    }

    /**
     * Synchronously loads all domains by iterating all available pages.
     *
     * @param client The Graph client
     * @return The list of applications
     */
    @Nonnull
    @AzureOperation("azure/aad.load_domains")
    static List<Domain> loadDomains(@Nonnull GraphServiceClient<Request> client) {
        LOG.debug("loading list of domains");

        var domains = new ArrayList<Domain>();

        var request = client.domains().buildRequest();
        while (true) {
            var page = request.get();
            if (page == null) {
                break;
            }
            domains.addAll(page.getCurrentPage());

            var nextPage = page.getNextPage();
            if (nextPage == null) {
                break;
            }

            LOG.debug("requesting next page of domains");
            request = nextPage.buildRequest();
        }

        LOG.debug("Found domains: " + domains.size());
        return domains;
    }

    /**
     * Synchronously loads all applications by iterating all available pages.
     *
     * @param client The Graph client
     * @return The list of applications
     */
    @Nonnull
    @AzureOperation("azure/aad.load_applications")
    static List<Application> loadApplications(@Nonnull GraphServiceClient<Request> client) {
        LOG.debug("loading list of applications");

        var applications = new ArrayList<Application>();

        var request = client.applications().buildRequest();
        while (true) {
            var page = request.get();
            if (page == null) {
                break;
            }
            applications.addAll(page.getCurrentPage());

            var nextPage = page.getNextPage();
            if (nextPage == null) {
                break;
            }

            LOG.debug("requesting next page of applications");
            request = nextPage.buildRequest();
        }

        LOG.debug("Found applications: " + applications.size());
        return applications;
    }

    @AzureOperation(value = "azure/aad.create_graph_client.subscription", params = "subscription.getName()")
    static GraphServiceClient<Request> createGraphClient(@Nonnull Subscription subscription) {
        LOG.debug("Creating new graph client for subscription " + subscription.getName());

        var account = Azure.az(AzureAccount.class).account();
        var credentials = account.getTokenCredential(subscription.getId());
        var authProvider = new TokenCredentialAuthProvider(credentials);

        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    static boolean isValidCallbackURL(@Nonnull String value) {
        try {
            var uri = URI.create(value);
            var scheme = uri.getScheme();
            return ("https".equals(scheme) || "http".equals(scheme)) && !uri.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
