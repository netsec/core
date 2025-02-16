package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.portlets.personas.model.Persona;
import com.liferay.util.StringPool;
import org.apache.velocity.context.Context;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rendering.velocity.directive.RenderParams;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotcms.rendering.velocity.servlet.VelocityModeHandler;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Optional;

/**
 * Builder of {@link HTMLPageAssetRendered}
 */
public class HTMLPageAssetRenderedBuilder {
    private IHTMLPage htmlPageAsset;
    private User user;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Host site;

    private final PermissionAPI  permissionAPI;
    private final UserAPI        userAPI;
    private final ContentletAPI  contentletAPI;
    private final LayoutAPI      layoutAPI;
    private final VersionableAPI versionableAPI;
    private final MultiTreeAPI   multiTreeAPI;

    public HTMLPageAssetRenderedBuilder() {

        this.permissionAPI  = APILocator.getPermissionAPI();
        this.userAPI        = APILocator.getUserAPI();
        this.contentletAPI  = APILocator.getContentletAPI();
        this.layoutAPI      = APILocator.getLayoutAPI();
        this.versionableAPI = APILocator.getVersionableAPI();
        this.multiTreeAPI   = APILocator.getMultiTreeAPI();
    }

    public HTMLPageAssetRenderedBuilder setHtmlPageAsset(final IHTMLPage htmlPageAsset) {
        this.htmlPageAsset = htmlPageAsset;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setUser(final User user) {
        this.user = user;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setRequest(final HttpServletRequest request) {
        this.request = request;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setResponse(final HttpServletResponse response) {
        this.response = response;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setSite(final Host site) {
        this.site = site;
        return this;
    }

    @CloseDBIfOpened
    public PageView build(final boolean rendered, final PageMode mode) throws DotDataException, DotSecurityException {
        final ContentletVersionInfo info = APILocator.getVersionableAPI().
                getContentletVersionInfo(htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId());

        final HTMLPageAssetInfo htmlPageAssetInfo = getHTMLPageAssetInfo(info);
        final Set<String> pagePersonalizationSet  = this.multiTreeAPI.getPersonalizationsForPage(htmlPageAsset.getIdentifier());
        final Template template = getTemplate(mode);
        final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);

        final TemplateLayout layout = template != null && template.isDrawed() && !LicenseManager.getInstance().isCommunity()
                ? DotTemplateTool.themeLayout(template.getInode()) : null;

        // this forces all velocity dotParses to use the site for the given page 
        // (unless host is specified in the dotParse) github 14624
        final RenderParams params=new RenderParams(user,language, site, mode);
        request.setAttribute(RenderParams.RENDER_PARAMS_ATTRIBUTE, params);
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final boolean canEditTemplate = this.permissionAPI.doesUserHavePermission(template, PermissionLevel.EDIT.getType(), user);
        final boolean canCreateTemplates = layoutAPI.doesUserHaveAccessToPortlet("templates", user);

        final PageRenderUtil pageRenderUtil = new PageRenderUtil(
                htmlPageAssetInfo.getPage(), systemUser, mode, language.getId(), this.site);

        if (!rendered) {
            final Collection<? extends ContainerRaw> containers =  pageRenderUtil.getContainersRaw();
            return new PageView(site, template, containers, htmlPageAssetInfo, layout, canCreateTemplates,
                    canEditTemplate, this.getViewAsStatus(mode, pagePersonalizationSet));
        } else {
            final Context velocityContext  = pageRenderUtil
                    .addAll(VelocityUtil.getInstance().getContext(request, response));
            final Collection<? extends ContainerRaw> containers = new ContainerRenderedBuilder(
                    pageRenderUtil.getContainersRaw(), velocityContext, mode).build();
            final String pageHTML = this.getPageHTML();
            return new HTMLPageAssetRendered(site, template, containers, htmlPageAssetInfo, layout, pageHTML,
                    canCreateTemplates, canEditTemplate, this.getViewAsStatus(mode, pagePersonalizationSet)
            );
        }
    }

    @CloseDBIfOpened
    public String getPageHTML() throws DotSecurityException, DotDataException {

        final PageMode mode = PageMode.get(request);

        if(mode.isAdmin ) {
            APILocator.getPermissionAPI().checkPermission(htmlPageAsset, PermissionLevel.READ, user);
        }

        return VelocityModeHandler.modeHandler(mode, request, response, htmlPageAsset.getURI(), site).eval();
    }

    private Template getTemplate(final PageMode mode) throws DotDataException {
        try {
            final User systemUser = APILocator.getUserAPI().getSystemUser();

            return mode.showLive ?
                    (Template) this.versionableAPI.findLiveVersion(htmlPageAsset.getTemplateId(), systemUser, mode.respectAnonPerms) :
                    (Template) this.versionableAPI.findWorkingVersion(htmlPageAsset.getTemplateId(), systemUser, mode.respectAnonPerms);
        } catch (DotSecurityException e) {
            return null;
        }
    }

    private HTMLPageAssetInfo getHTMLPageAssetInfo(final ContentletVersionInfo info) throws DotDataException {
        HTMLPageAssetInfo htmlPageAssetInfo = new HTMLPageAssetInfo()
            .setPage((HTMLPageAsset) this.htmlPageAsset)
            .setWorkingInode(info.getWorkingInode())
            .setShortyWorking(APILocator.getShortyAPI().shortify(info.getWorkingInode()))
            .setCanEdit(this.permissionAPI.doesUserHavePermission(htmlPageAsset, PermissionLevel.EDIT.getType(), user, false))
            .setCanRead(this.permissionAPI.doesUserHavePermission(htmlPageAsset, PermissionLevel.READ.getType(), user, false))
            .setLiveInode(info.getLiveInode())
            .setShortyLive(APILocator.getShortyAPI().shortify(info.getLiveInode()))
            .setCanLock(this.canLock());

        final String lockedBy= (info.getLockedBy()!=null)  ? info.getLockedBy() : null;

        if(lockedBy!=null) {
            htmlPageAssetInfo.setLockedOn(info.getLockedOn())
                .setLockedBy(lockedBy)
                .setLockedByName(getLockedByUserName(info));
        }

        return htmlPageAssetInfo;
    }

    private String getLockedByUserName(final ContentletVersionInfo info) throws DotDataException {
        try {
            return userAPI.loadUserById(info.getLockedBy()).getFullName();
        } catch (DotSecurityException e) {
            return null;
        }
    }

    private boolean canLock()  {
        try {
            APILocator.getContentletAPI().canLock((HTMLPageAsset) htmlPageAsset, user);
            return true;
        } catch (DotLockException e) {
            return false;
        }
    }

    private ViewAsPageStatus getViewAsStatus(final PageMode pageMode, final Set<String> pagePersonalizationSet)
            throws DotDataException {

        final IPersona persona     = this.getCurrentPersona();
        final boolean personalized = this.isPersonalized(persona, pagePersonalizationSet);

        return new ViewAsPageStatus()
            .setPersonalized(personalized)
            .setPersona(this.getCurrentPersona())
            .setLanguage(WebAPILocator.getLanguageWebAPI().getLanguage(request))
            .setDevice(getCurrentDevice())
            .setPageMode(pageMode);
    }

    private boolean isPersonalized (final IPersona persona, final Set<String> pagePersonalizationSet) {

        return null != persona?
                pagePersonalizationSet.contains
                    (Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + persona.getKeyTag()): false;
    }

    private IPersona getCurrentPersona() {
        final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request);
        return visitor.isPresent() && visitor.get().getPersona() != null ? visitor.get().getPersona() : null;
    }

    private Contentlet getCurrentDevice() throws DotDataException {
        final String deviceInode = (String) request.getSession().getAttribute(WebKeys.CURRENT_DEVICE);
        Contentlet currentDevice = null;

        try {

            final String currentDeviceId = deviceInode == null ?
                    (String) request.getSession().getAttribute(WebKeys.CURRENT_DEVICE)
                    : deviceInode;

            if (currentDeviceId != null) {
                currentDevice = contentletAPI.find(currentDeviceId, user, false);

                if (currentDevice == null) {
                    request.getSession().removeAttribute(WebKeys.CURRENT_DEVICE);
                }
            }
        } catch (DotSecurityException e) {
            Logger.debug(this.getClass(),
                    "Exception on createViewAsMap exception message: " + e.getMessage(), e);
        }

        return currentDevice;
    }
}
