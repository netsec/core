package com.dotmarketing.portlets.cmsmaintenance.action;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Clickstream404;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionReference;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.calendar.model.CalendarReminder;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm;
import com.dotmarketing.portlets.cmsmaintenance.util.AssetFileNameFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotReindexStateException;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardUserPreferences;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.util.RulesImportExportUtil;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.HibernateCollectionConverter;
import com.dotmarketing.util.HibernateMapConverter;
import com.dotmarketing.util.ImportExportUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.ZipUtil;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.ejb.ImageLocalManagerUtil;
import com.liferay.portal.ejb.PortletPreferencesLocalManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * This class group all the CMS Maintenance Task
 * (Cache controll, search and replace, import/export content )
 *
 * @author Oswaldo
 * @version 3.3
 *
 */
public class ViewCMSMaintenanceAction extends DotPortletAction {

	/**
	 * The path where backup files are stored
	 */
	String backupFilePath = ConfigUtils.getBackupPath();

	/**
	 * The path where tmp files are stored. This gets wiped alot
	 */
	String backupTempFilePath = ConfigUtils.getBackupPath()+File.separator+"temp";
	private static String assetRealPath = null;
	private static String assetPath = File.separator + "assets";

	private ContentletAPI conAPI = APILocator.getContentletAPI();

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
	throws Exception {
		Logger.debug(this, "Running ViewCMSMaintenanceAction!!!! = " + req.getWindowState());

		// Set the asset paths
        try {
        	assetRealPath = Config.getStringProperty("ASSET_REAL_PATH");
        } catch (Exception e) { }
        try {
            assetPath = Config.getStringProperty("ASSET_PATH");
        } catch (Exception e) { }

		try {
			//gets the user
			_initCacheValues(req);

			if (req.getWindowState().equals(WindowState.MAXIMIZED)) {
				Logger.debug(this, "Showing view action cms maintenance maximized");
				_initStructures(form,req,res);
				return mapping.findForward("portlet.ext.cmsmaintenance.view_cms_maintenance");
			} else {
				Logger.debug(this, "Showing view action cms maintenance minimized");
				return mapping.findForward("portlet.ext.cmsmaintenance.view");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(com.liferay.portal.util.Constants.COMMON_ERROR);
		}
	}

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
			throws Exception
	{
		String message = "";
		String errorMessage = "";
		CmsMaintenanceForm ccf = (CmsMaintenanceForm) form;
		String cmd = req.getParameter("cmd");
		String defaultStructure = req.getParameter("defaultStructure");
		Structure structure = new Structure();

		//Manage all the cache Task
		if(cmd.equals("cache")){

			String cacheName = ccf.getCacheName();
			if (cacheName.equals(com.dotmarketing.util.WebKeys.Cache.CACHE_CONTENTS_INDEX))
			{
				Logger.info(this, "Running Contents Index Cache");
				if(defaultStructure.equals("Rebuild Whole Index")){
					//structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(defaultStructure);
				}else
					structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(ccf.getStructure());
				if(!InodeUtils.isSet(structure.getInode()))
				{

						int shards = Config.getIntProperty("es.index.number_of_shards", 2);
						try{
							shards = Integer.parseInt(req.getParameter("shards"));
						}catch(Exception e){

						}
						System.setProperty("es.index.number_of_shards", String.valueOf(shards));
						Logger.info(this, "Running Contentlet Reindex");

						conAPI.refreshAllContent();

						message = "message.cmsmaintenance.cache.indexrebuilt";
						AdminLogger.log(ViewCMSMaintenanceAction.class, "processAction", "Running Contentlet Reindex");

				}
				else
				{
					try{
						Logger.info(this, "Running Contentlet Reindex on Structure: " + structure.getName());
						conAPI.reindex(structure);
						message = "message.cmsmaintenance.cache.indexrebuilt";
						AdminLogger.log(ViewCMSMaintenanceAction.class, "processAction", "Running Contentlet Reindex on structure : "+structure.getName());
					}catch(DotReindexStateException dre){
						Logger.warn(this, "Content Reindexation Failed caused by: "+ dre.getMessage());
						errorMessage = "message.cmsmaintenance.cache.failedtorebuild";
					}
				}
			} else if (cacheName.equals(com.dotmarketing.util.WebKeys.Cache.CACHE_MENU_FILES))
			{
				Logger.info(this, "Deleting Menu Files");
				_deleteMenusCache();
				message = "message.cmsmaintenance.cache.flushmenucaches";
			} else if (cacheName.equals("flushCache"))
			{
				final String cacheToFlush = req.getParameter("cName");
				boolean isAllCachesFlush = false;// this boolean is for the messages (logs and UI)
				try{
					CacheLocator.getCache(cacheToFlush);
				}catch (NullPointerException e) {
					isAllCachesFlush = true;//is a NPE is returned means it's cleaning all the caches
				}
				final String msgLogger = isAllCachesFlush ? "Flushing All Caches" : "Flushing " + cacheToFlush +" Cache";
				Logger.info(this, msgLogger);
				_flush(cacheToFlush);
				message = isAllCachesFlush ? "message.cmsmaintenance.cache.flushallcache" : "message.cmsmaintenance.cache.flushcache";
			} else {
				Logger.info(this, "Flushing Live and Working File Cache");
				_deleteFiles(com.dotmarketing.util.WebKeys.Cache.CACHE_LIVE_FILES);
				_deleteFiles(com.dotmarketing.util.WebKeys.Cache.CACHE_WORKING_FILES);

				message = "message.cmsmaintenance.cache.deletefiles";
			}
		} // Download the records that could not be re-indexed in a CSV file
		else if (cmd.equals("export-failed-as-csv")) {
			ActionResponseImpl resImpl = (ActionResponseImpl) res;
			HttpServletResponse response = resImpl.getHttpServletResponse();
			downloadRemainingRecordsAsCsv(response);
		} //Manage all the search and replace Task
		else if(cmd.equals("searchandreplace")){
			Logger.info(this, "Running Search & Replace");
			Map<String,String> result = searchAndReplace(ccf);
			if(result.get("type") != null){
				if(result.get("type").equals("message")){
					message = "Search and Replace complete <br>" + result.get("message");
				} else {
					errorMessage = "Search and Replace complete <br>" + result.get("message");
				}
				_deleteFiles(com.dotmarketing.util.WebKeys.Cache.CACHE_LIVE_FILES);
				_deleteFiles(com.dotmarketing.util.WebKeys.Cache.CACHE_WORKING_FILES);
				_flush(com.dotmarketing.util.WebKeys.Cache.CACHE_ALL_CACHES);
			}
		}
		//Not being used this is being called using ajax
		else if(cmd.equals("fixAssetsInconsistencies")){
           // this one NOT
		}
		//Not being used this is being called using ajax
		else if(cmd.equals("dropoldassets")){

			Logger.info(this, "Running Drop old Assets");
			if(_dropAssetOldVersions(ccf) >= 0){
				message = "message.cmsmaintenance.dropoldassets.sucessfully";
			}else{
				errorMessage = "message.cmsmaintenance.dropoldassets.failed";
			}

		}
		//Manage all the import/ export Task
		else {
			MaintenanceUtil.fixImagesTable();
			File f = new File(backupFilePath);
			f.mkdirs();
			f = new File(backupTempFilePath);
			f.mkdirs();
			deleteTempFiles();
			boolean dataOnly = Parameter.getBooleanFromString(req.getParameter("dataOnly"), true);

			if(cmd.equals("createZip")) {
				if(!dataOnly){
					moveAssetsToBackupDir();
				}
				message = "Creating XML Files. ";
				createXMLFiles();
				String x = UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
				File zipFile = new File(backupFilePath + "/backup_" + x + "_.zip");
				message +="Zipping up to file:" + zipFile.getAbsolutePath();
				final BufferedOutputStream bout = new BufferedOutputStream(Files.newOutputStream(zipFile.toPath()));

				zipTempDirectoryToStream(bout);
				message +=". Done.";

//			}else if(cmd.equals("wipeOutDotCMSDatabase")) {
//
//				message = "Deleted dotCMS Database";
//				deleteDotCMS();

			}else if(cmd.equals("downloadZip")) {

				message ="File Downloaded";
				String x = UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
				File zipFile = new File(backupFilePath + "/backup_" + x + "_.zip");

				ActionResponseImpl responseImpl = (ActionResponseImpl) res;
				HttpServletResponse httpResponse = responseImpl.getHttpServletResponse();
				httpResponse.setHeader("Content-type", "");
				httpResponse.setHeader("Content-Disposition", "attachment; filename=" + zipFile.getName());

				if(!dataOnly){
					moveAssetsToBackupDir();
				}

				createXMLFiles();

				zipTempDirectoryToStream(httpResponse.getOutputStream());

			}else if(cmd.equals("upload")) {


				UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);
				ActionResponseImpl responseImpl = (ActionResponseImpl) res;
				HttpServletResponse httpResponse = responseImpl.getHttpServletResponse();

				ActionRequestImpl requestImpl = (ActionRequestImpl) req;
				HttpServletRequest httpRequest = requestImpl.getHttpServletRequest();

				//message ="file upload Done.";

				doUpload(httpRequest, httpResponse, uploadReq);
			}
		}

		if(UtilMethods.isSet(message)){
			SessionMessages.add(req, "message",message);
		}
		if(UtilMethods.isSet(errorMessage)){
			SessionMessages.add(req, "error",errorMessage);
		}

		String referer = req.getParameter("referer");
		setForward(req,referer);
	}

	private void _flush(String cacheName)throws Exception{
			try{
				CacheLocator.getCache(cacheName).clearCache();
			}catch (NullPointerException e) {
				MaintenanceUtil.flushCache();
			}
		APILocator.getPermissionAPI().resetAllPermissionReferences();
	}

	private void _deleteMenusCache()throws Exception{
		MaintenanceUtil.deleteMenuCache();
	}

	private void _deleteFiles(String cacheName)
	throws Exception
	{
		try
		{
			String realPath = "";

			String velocityRootPath =ConfigUtils.getDynamicVelocityPath();
			if (velocityRootPath.startsWith(File.separator + "WEB-INF")) {
				velocityRootPath = FileUtil.getRealPath(velocityRootPath);
			}

			if (cacheName.equals(com.dotmarketing.util.WebKeys.Cache.CACHE_LIVE_FILES))
			{
				realPath = velocityRootPath + File.separator + "live";
			}
			else
			{
				realPath = velocityRootPath + File.separator + "working";
			}
			File file = new File(realPath);
			if (file.isDirectory())
			{
				_deleteRoot(file);
				Logger.debug(ViewCMSMaintenanceAction.class,"The directory " + realPath + " has been deleted");
			}
		}
		catch(Exception ex)
		{
			Logger.error(ViewCMSMaintenanceAction.class,ex.toString());
			throw ex;
		}
	}

	private boolean _deleteRoot(File root) throws Exception
	{
		boolean returnValue = true;
		File[] childs = root.listFiles();
		for(int i = 0; i < childs.length; i++)
		{
			File child = childs[i];
			if (child.isFile())
			{
				returnValue = returnValue && child.delete();
			}
			if (child.isDirectory())
			{
				returnValue = returnValue && _deleteRoot(child);
			}
		}
		return returnValue;
	}

	private int _numberRoot(File root) throws Exception
	{
		int returnValue = 0;
		File[] childs = root.listFiles();
		for(int i = 0; i < childs.length; i++)
		{
			File child = childs[i];
			if (child.isFile())
			{
				returnValue++;
			}
			if (child.isDirectory())
			{
				returnValue += _numberRoot(child);
			}
		}
		return returnValue;
	}

	/**
	 * Initialice cache values
	 * @param req
	 */
	private void _initCacheValues(RenderRequest req)
	{
		int liveCount = 0;
		int workingCount = 0;
		try
		{
			Logger.debug(this, "Initializing Cache Values");
			String velocityRootPath =ConfigUtils.getDynamicVelocityPath();
			if (velocityRootPath.startsWith(File.separator + "WEB-INF")) {
				velocityRootPath = FileUtil.getRealPath(velocityRootPath);
			}
			String livePath = velocityRootPath + File.separator + "live";
			String workingPath = velocityRootPath + File.separator + "working";

			//Count the Live Files
			File file = new File(livePath);
			liveCount = _numberRoot(file);
			Logger.debug(this, "Found "+liveCount+" live Files");

			//Count the working files
			file = new File(workingPath);
			workingCount = _numberRoot(file);
			Logger.debug(this, "Found "+workingCount+" working Files");
		}
		catch(Exception ex)
		{
			Logger.error(ViewCMSMaintenanceAction.class,"Error calculating the number of files");
		}
		finally
		{
			req.setAttribute(WebKeys.Cache.CACHE_NUMBER_LIVE_FILES,new Integer(liveCount));
			req.setAttribute(WebKeys.Cache.CACHE_NUMBER_WORKING_FILES,new Integer(workingCount));
		}
	}


	private Map<String,String> searchAndReplace(CmsMaintenanceForm form) throws DotDataException{

		String message="";
		Map<String,String> messageResult = new HashMap<String,String>();
		boolean isAdmin = false;

		if (UtilMethods.isSet(form.getUserId())) {
			try {
				if (APILocator.getUserAPI().isCMSAdmin(APILocator.getUserAPI().loadUserById(form.getUserId(),APILocator.getUserAPI().getSystemUser(),false))) {
					isAdmin = true;
				}
			} catch (NoSuchUserException e) {
				Logger.error(ViewCMSMaintenanceAction.class,"USER DOESN'T EXIST" + e.getMessage(),e);
				message = "Cannot find user";
			} catch (DotSecurityException e) {
				message = "Failed to check for permissions";
				Logger.error(ViewCMSMaintenanceAction.class,e.getMessage(),e);
			}
		}else{
			message = "User not passed";
			Logger.error(ViewCMSMaintenanceAction.class,"No UserId Passed");
		}

		if (isAdmin) {
			String searchString = form.getSearchString();
			String replaceString = form.getReplaceString();
			boolean DBerrorFound = MaintenanceUtil.DBSearchAndReplace(searchString, replaceString);
			if(DBerrorFound){
				message = "Search/Replace Finished with Errors.  See Log for more info";
			}else{
				message = "Search/Replace Finished Successful";
			}
			messageResult.put("type", "message");
			messageResult.put("message", message);
		}
		return messageResult;
	}

	/**
	 * Does what it says - deletes all files from the backupTempFilePath
	 * @author Will
	 */
	private void deleteTempFiles() {
		Logger.info(this, "Deleting Temporary Files");
		File f = new File(backupTempFilePath);
		String[] _tempFiles = f.list();
		Logger.info(this, "Found "+_tempFiles.length+" Files");
		int count = 0;
		for (int i = 0; i < _tempFiles.length; i++) {
			f = new File(backupTempFilePath + File.separator + "" + _tempFiles[i]);
				if(f.isDirectory()){
					FileUtil.deltree(f);
				}
			count+=1;
			f.delete();
		}
		Logger.info(this, "Deleted " + count + " Files");
	}

	private void moveAssetsToBackupDir() throws FileNotFoundException, IOException{
		String assetDir;
		File backupDir = new File(backupTempFilePath);
		backupDir.mkdirs();
		Logger.info(this, "Moving assets to back up directory: " + backupTempFilePath);
		if(!UtilMethods.isSet(assetRealPath)){
			assetDir = FileUtil.getRealPath(assetPath);
		}else{
			assetDir = assetRealPath;
		}
		FileUtil.copyDirectory(assetDir, backupTempFilePath + File.separator + "asset", new AssetFileNameFilter());

		//do not ship the license.
		String f = backupTempFilePath + File.separator + "asset" + File.separator + "license";
		FileUtil.deltree(f);

            String d = backupTempFilePath + File.separator + "asset" + File.separator + "dotGenerated";
		FileUtil.deltree(d);
	}


	/**
	 * This method will pull a list of all tables /classed being managed by
	 * hibernate and export them, one class per file to the backupTempFilePath
	 * as valid XML. It uses XStream to write the xml out to the files.
	 *
	 * @throws ServletException
	 * @throws IOException
	 * @author Will
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	private void createXMLFiles() throws ServletException, IOException, DotDataException, DotSecurityException {

		Logger.info(this, "Starting createXMLFiles()");

		Set<Class> _tablesToDump = new HashSet<Class>();
		try {

			/* get a list of all our tables */
			Map map = new HashMap();
			//Including Identifier.class and Language.class because it is not mapped with Hibernate anymore
			map.put(Identifier.class, null);
			map.put(Language.class, null);
			map.put(Relationship.class, null);
			map.putAll(HibernateUtil.getSession().getSessionFactory().getAllClassMetadata());

			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				Class x = (Class) pairs.getKey();
				if (!x.equals(Inode.class) && !x.equals(Clickstream.class) && !x.equals(ClickstreamRequest.class) && !x.equals(Clickstream404.class))
					_tablesToDump.add(x);

			}

			XStream _xstream = null;
			HibernateUtil _dh = null;
			DotConnect dc = null;
			List _list = null;
			File _writing = null;
			BufferedOutputStream _bout = null;

			for (Class clazz : _tablesToDump) {
				if(clazz.equals(Structure.class) || clazz.equals(Field.class) || clazz.equals(FieldVariable.class)){
					continue;
				}

				//http://jira.dotmarketing.net/browse/DOTCMS-5031
				if(PermissionReference.class.equals(clazz)){
					continue;
				}

				_xstream = new XStream(new DomDriver());

				//http://jira.dotmarketing.net/browse/DOTCMS-6059
				if(clazz.equals(DashboardSummary404.class) || clazz.equals(DashboardUserPreferences.class)){
					_xstream.addDefaultImplementation(com.dotcms.repackage.net.sf.hibernate.collection.Set.class, java.util.Set.class);
					_xstream.addDefaultImplementation(com.dotcms.repackage.net.sf.hibernate.collection.List.class, java.util.List.class);
					_xstream.addDefaultImplementation(com.dotcms.repackage.net.sf.hibernate.collection.Map.class, java.util.Map.class);
					Mapper mapper = _xstream.getMapper();
					_xstream.registerConverter(new HibernateCollectionConverter(mapper));
					_xstream.registerConverter(new HibernateMapConverter(mapper));
				}

				/*
				 * String _shortClassName =
				 * clazz.getName().substring(clazz.getName().lastIndexOf("."),clazz.getName().length());
				 * xstream.alias(_shortClassName, clazz);
				 */
				int i= 0;
				int step = 1000;
				int total =0;
				java.text.NumberFormat formatter = new java.text.DecimalFormat("0000000000");
				/* we will only export 10,000,000 items of any given type */
				for(i=0;i < 10000000;i=i+step){

					_dh = new HibernateUtil(clazz);
					_dh.setFirstResult(i);
					_dh.setMaxResults(step);

					//This line was previously like;
					//_dh.setQuery("from " + clazz.getName() + " order by 1,2");
					//This caused a problem when the database is Oracle because Oracle causes problems when the results are ordered
					//by an NCLOB field. In the case of dot_containers table, the second field, CODE, is an NCLOB field. Because of this,
					//ordering is done only on the first field for the tables, which is INODE
					if(com.dotmarketing.beans.Tree.class.equals(clazz)){
						_dh.setQuery("from " + clazz.getName() + " order by parent, child, relation_type");
					}
					else if(MultiTree.class.equals(clazz)){
						_dh.setQuery("from " + clazz.getName() + " order by parent1, parent2, child, relation_type");
					}
					else if(TagInode.class.equals(clazz)){
						_dh.setQuery("from " + clazz.getName() + " order by inode, tag_id");
					}
					else if(Tag.class.equals(clazz)){
						_dh.setQuery("from " + clazz.getName() + " order by tag_id, tagname");
					}
					else if(CalendarReminder.class.equals(clazz)){
						_dh.setQuery("from " + clazz.getName() + " order by user_id, event_id, send_date");
					}
					else if(Identifier.class.equals(clazz)){
						dc = new DotConnect();
						dc.setSQL("select * from identifier order by parent_path, id")
								.setStartRow(i).setMaxRows(step);
					}
					else if (Language.class.equals(clazz)) {
						dc = new DotConnect();
						dc.setSQL("SELECT * FROM language order by id")
								.setStartRow(i).setMaxRows(step);
					}
					else if (Relationship.class.equals(clazz)) {
						dc = new DotConnect();
						dc.setSQL("SELECT * FROM relationship order by inode")
								.setStartRow(i).setMaxRows(step);
					}
					else {
						_dh.setQuery("from " + clazz.getName() + " order by 1");
					}

					if(Identifier.class.equals(clazz)){
						_list = TransformerLocator
								.createIdentifierTransformer(dc.loadObjectResults()).asList();
					} else if (Language.class.equals(clazz)) {
						_list = TransformerLocator
								.createLanguageTransformer(dc.loadObjectResults()).asList();
					} else if (Relationship.class.equals(clazz)) {
						_list = TransformerLocator
								.createRelationshipTransformer(dc.loadObjectResults()).asList();
					} else {
						_list = _dh.list();
					}

					if(_list.size() ==0){
						try {
							_bout.close();
						}
						catch( java.lang.NullPointerException npe){}
						_bout = null;

						break;
					}

					if(_list != null && _list.size() > 0 && _list.get(0) instanceof Comparable){
						java.util.Collections.sort(_list);
					}

					_writing = new File(backupTempFilePath + "/" + clazz.getName() + "_" + formatter.format(i) + ".xml");
					_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));

					total = total + _list.size();

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						Logger.warn(this, "An error ocurred trying to create XML files");
						Logger.error(this,e.getMessage(),e);
					}

					try {
						_xstream.toXML(_list, _bout);
					} finally {
						_bout.close();
					}

					_bout = null;

				}
				Logger.info(this, "writing : " + total + " records for " + clazz.getName());
			}


			/* Run Liferay's Tables */
			/* Companies */
			_list = PublicCompanyFactory.getCompanies();
			List<Company> companies = new ArrayList<Company>(_list);
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/" + Company.class.getName() + ".xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* Users */
			_list = APILocator.getUserAPI().findAllUsers();
			_list.add(APILocator.getUserAPI().getDefaultUser());
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/" + User.class.getName() + ".xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* users_roles */
			dc = new DotConnect();

			/* counter */
			dc.setSQL("select * from counter");
			_list = dc.getResults();
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Counter.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* counter */
			dc.setSQL("select * from address");
			_list = dc.getResults();
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Address.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* pollschoice */
			dc.setSQL("select * from pollschoice");
			_list = dc.getResults();
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Pollschoice.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* pollsdisplay */
			dc.setSQL("select * from pollsdisplay");
			_list = dc.getResults();
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Pollsdisplay.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* pollsquestion */
			dc.setSQL("select * from pollsquestion");
			_list = dc.getResults();
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Pollsquestion.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* pollsvote */
			dc.setSQL("select * from pollsvote");
			_list = dc.getResults();
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Pollsvote.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* image */
			_list = ImageLocalManagerUtil.getImages();

			/*
			 * The changes in this part were made for Oracle databases. Oracle has problems when
			 * getString() method is called on a LONG field on an Oracle database. Because of this,
			 * the object is loaded from liferay and DotConnect is not used
			 * http://jira.dotmarketing.net/browse/DOTCMS-1911
			 */

			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Image.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* portlet */

			/*
			 * The changes in this part were made for Oracle databases. Oracle has problems when
			 * getString() method is called on a LONG field on an Oracle database. Because of this,
			 * the object is loaded from liferay and DotConnect is not used
			 * http://jira.dotmarketing.net/browse/DOTCMS-1911
			 */
			dc.setSQL("select * from portlet");
			_list = dc.getResults();
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Portlet.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			/* portlet_preferences */

			try{
				_list = PortletPreferencesLocalManagerUtil.getPreferences();
			}catch(Exception e){
				Logger.error(this,"Error in retrieveing all portlet preferences");
			}
			_xstream = new XStream(new DomDriver());
			_writing = new File(backupTempFilePath + "/Portletpreferences.xml");
			_bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
			try {
				_xstream.toXML(_list, _bout);
			} finally {
				_bout.close();
			}

			//backup content types
			File file = new File(backupTempFilePath + File.separator + "ContentTypes-" + ContentTypeImportExportUtil.CONTENT_TYPE_FILE_EXTENSION);
			new ContentTypeImportExportUtil().exportContentTypes(file);

			file = new File(backupTempFilePath + "/WorkflowSchemeImportExportObject.json");
			WorkflowImportExportUtil.getInstance().exportWorkflows(file);

			file = new File(backupTempFilePath + "/RuleImportExportObject.json");
			RulesImportExportUtil.getInstance().export(file);

			IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

			file = new File(backupTempFilePath + "/index_working.json");
			new ESIndexAPI().backupIndex(info.working, file);


			file = new File(backupTempFilePath + "/index_live.json");
			new ESIndexAPI().backupIndex(info.live, file);

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}


	/**
	 * Will zip up all files in the tmp directory and send the result to the
	 * given OutputStream
	 *
	 * @param out
	 *            OutputStream to write the zip files to
	 * @throws IOException
	 * @author Will
	 */
	private void zipTempDirectoryToStream(OutputStream out) throws IOException {
		byte b[] = new byte[512];
		ZipOutputStream zout = new ZipOutputStream(out);
		ZipUtil.zipDirectory(backupTempFilePath, zout);
		zout.close();
		out.close();
	}

	/**
	 * Handles the file upload for the Servlet. It will send files to be
	 * unzipped if nessary
	 *
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void doUpload(HttpServletRequest request, HttpServletResponse response, UploadPortletRequest upr) throws IOException {
		Logger.info(this, "Uploading File");
		File importFile = upr.getFile("fileUpload");
		PrintWriter out = response.getWriter();
		ImportExportUtil ieu = new ImportExportUtil();
		if(ieu.validateZipFile(importFile)){
			request.getSession().invalidate();
			MaintenanceUtil.flushCache();
			ieu.doImport(out);
			SessionMessages.add(request, "message", "File-Upload-Done");
		}else{
			SessionMessages.add(request, "error",  "File-Upload-Failed");
		}

	}

	public void _initStructures(ActionForm form,RenderRequest req, RenderResponse res)
	{
		CmsMaintenanceForm maintenanceForm = (CmsMaintenanceForm) form;
		List<Structure> structures = ImmutableList.of();
		//List<Structure> structures = StructureFactory.getStructures();
		//maintenanceForm.setStructures(structures);
	}

	@SuppressWarnings("unchecked")
	private int _dropAssetOldVersions(CmsMaintenanceForm ccf) throws ParseException	{

		Date assetsOlderThan = new SimpleDateFormat("MM/dd/yyyy").parse(ccf.getRemoveassetsdate());
		return CMSMaintenanceFactory.deleteOldAssetVersions(assetsOlderThan);

	}

	/**
	 * Retrieves the basic information of the contents in the
	 * {@code dist_reindex_journal} table that could not be re-indexed and sends
	 * it back to the user as a CSV file. This way users can keep track of them
	 * and check the logs to get more information about the failure.
	 *
	 * @param response
	 *            - The {@link HttpServletResponse} object that allows to send
	 *            the CSV file to the user.
	 */
	private void downloadRemainingRecordsAsCsv(HttpServletResponse response) {
		String fileName = "failed_reindex_records" + new java.util.Date().getTime();
		String[] fileColumns = new String[] { "ID", "Identifier To Index", "Priority", "Cause" };
		PrintWriter pr = null;
		try {
			response.setContentType("application/octet-stream; charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".csv\"");
			pr = response.getWriter();
			pr.print(StringUtils.join(fileColumns, ","));
			pr.print("\r\n");
			DotConnect dc = new DotConnect();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT drj.id, drj.ident_to_index, drj.inode_to_index, drj.priority, drj.index_val ")
					.append("FROM dist_reindex_journal drj WHERE drj.priority >= ")
					.append(ReindexQueueFactory.Priority.REINDEX.dbValue());
			dc.setSQL(sql.toString());
			List<Map<String, Object>> failedRecords = dc.loadObjectResults();
			if (!failedRecords.isEmpty()) {
				for (Map<String, Object> row : failedRecords) {
					StringBuilder entry = new StringBuilder();
					String id = null;
					String priority = null;
					if (DbConnectionFactory.isOracle()) {
						BigDecimal rowVal = (BigDecimal) row.get("id");
						id = new Long(rowVal.toPlainString()).toString();
						rowVal = (BigDecimal) row.get("priority");
						priority = new Long(rowVal.toPlainString()).toString();
					} else {
						Long rowVal = (Long) row.get("id");
						id = rowVal.toString();
						priority = String.valueOf((Integer) row.get("priority"));
					}
					entry.append(id).append(", ");
					entry.append(row.get("ident_to_index").toString()).append(", ");


					entry.append(priority).append(",");
					entry.append(row.get("index_val").toString());
					pr.print(entry.toString());
					pr.print("\r\n");
				}
			} else {
				Logger.debug(this, "Re-index table contained zero failed records. The CSV file will not be created.");
			}
		} catch (Exception e) {
			Logger.error(this, "Download of CSV file with remaining non-indexed records failed.", e);
		} finally {
			pr.flush();
			pr.close();
		}
	}
}
