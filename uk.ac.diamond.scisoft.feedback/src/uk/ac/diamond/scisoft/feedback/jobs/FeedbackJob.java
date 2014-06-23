/*-
 * Copyright 2013 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.diamond.scisoft.feedback.jobs;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.dawb.common.util.eclipse.BundleUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.LogConstants;
import uk.ac.diamond.scisoft.feedback.Activator;
import uk.ac.diamond.scisoft.feedback.FeedbackRequest;
import uk.ac.diamond.scisoft.feedback.attachment.AttachedFile;
import uk.ac.diamond.scisoft.feedback.utils.FeedbackConstants;
import uk.ac.diamond.scisoft.system.info.SystemInformation;

/**
 * 
 */
public class FeedbackJob extends Job {

	private static final Logger logger = LoggerFactory.getLogger(FeedbackJob.class);

	private String fromvalue;
	private String subjectvalue;
	private String messagevalue;
	private String emailvalue;
	private String destinationEmail;
	private List<AttachedFile> attachedFilesList;

	public FeedbackJob(String name, 
			String fromvalue, String subjectvalue, 
			String messagevalue, String emailvalue, 
			String destinationEmail, List<AttachedFile> attachedFilesList) {
		super(name);
		this.fromvalue = fromvalue;
		this.subjectvalue = subjectvalue;
		this.messagevalue = messagevalue;
		this.emailvalue = emailvalue;
		this.destinationEmail = destinationEmail;
		this.attachedFilesList = attachedFilesList;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			if (fromvalue == null || fromvalue.length() == 0) {
				fromvalue = "user";
			} else {
				if (Activator.getDefault() != null) {
					Activator.getDefault().getPreferenceStore().setValue(FeedbackConstants.FROM_PREF, fromvalue);
				}
			}

			if (monitor.isCanceled()) return Status.CANCEL_STATUS;

			String from = fromvalue;
			String subject = FeedbackConstants.DAWN_FEEDBACK + " - " + subjectvalue;
			if (subjectvalue != null && !"".equals(subjectvalue)) {
				if (Activator.getDefault() != null) {
					Activator.getDefault().getPreferenceStore().setValue(FeedbackConstants.SUBJ_PREF, subjectvalue);
				}
			}
			StringBuilder messageBody = new StringBuilder();
			String computerName = "Unknown";
			try {
				computerName = InetAddress.getLocalHost().getHostName();
			} finally {

			}
			messageBody.append("Machine is   : " + computerName + "\n");

			String versionNumber = "Unknown";
			try {
				versionNumber = BundleUtils.getDawnVersion();
			} catch (Exception e) {
				logger.debug("Could not retrieve product and system information:" + e);
			}

			if (monitor.isCanceled()) return Status.CANCEL_STATUS;

			messageBody.append("Version is   : " + versionNumber + "\n");
			messageBody.append(messagevalue);
			messageBody.append("\n\n\n");
			messageBody.append(SystemInformation.getSystemString());

			// get the mail to address from the properties
//			String mailTo = System.getProperty("uk.ac.diamond.scisoft.feedback.recipient", MAIL_TO);

			if (monitor.isCanceled()) return Status.CANCEL_STATUS;

			// fill the list of files to attach
			List<File> attachmentFiles = new ArrayList<File>();
			int totalSize = 0;
			// add user attached files
			for (int i = 0; i < attachedFilesList.size(); i++) {
				attachmentFiles.add(new File(attachedFilesList.get(i).path));
				// check that the size does not exceed the maximum one
				if (attachmentFiles.get(i).length() > FeedbackConstants.MAX_SIZE) {
					logger.error("The attachment file size exceeds: " + FeedbackConstants.MAX_SIZE);
					return new Status(IStatus.WARNING, "File Size Problem",
							"The attachment file size exceeds 10MB. Please chose a smaller file to attach.");
				}
				totalSize += attachmentFiles.get(i).length();
			}
			// add logs files
			List<File> logFiles = getLogFile();
			for (File file : logFiles) {
				if (file.length() > FeedbackConstants.MAX_SIZE) {
					logger.error("The log file size exceeds: " + FeedbackConstants.MAX_SIZE);
					return new Status(IStatus.WARNING, "File Size Problem",
							"The log file attached to the feedback exceeds 10MB.");
				}
				attachmentFiles.add(file);
				totalSize += file.length();
			}

			if (totalSize > FeedbackConstants.MAX_TOTAL_SIZE) {
				logger.error("The total size of your attachement files exceeds: " + FeedbackConstants.MAX_TOTAL_SIZE);
				return new Status(IStatus.WARNING, "File Size Problem",
						"The total size of your attachement files exceeds 20MB. Please chose smaller files to attach.");
			}

			if (monitor.isCanceled()) return Status.CANCEL_STATUS;

			// Test that the message is correctly formatted (not empty) and Test the email format
			if (!messagevalue.equals("") && emailvalue.contains("@")) {
				return FeedbackRequest.doRequest(from, destinationEmail,
						System.getProperty("user.name", "Unknown User"), subject,
						messageBody.toString(), attachmentFiles, monitor);
			}
			return new Status(IStatus.WARNING, "Format Problem",
					"Please type in your email and the message body before sending the feedback.");
		} catch (Exception e) {
			logger.error("Feedback email not sent", e);
			return new Status(
					IStatus.WARNING,
					"Feedback not sent!",
					"Please check that you have an Internet connection. If the feedback is still not working, click on OK to submit your feedback using the online feedback form available at http://dawnsci-feedback.appspot.com/");
		}
	}

	private List<File> getLogFile() {
		List<File> files = new ArrayList<File>(); 
		if (isWindowsOS()) {
			File fout = new File(System.getProperty("user.home")+ LogConstants.LOG_FOLDER + LogConstants.OUT_FILE);
			File ferr = new File(System.getProperty("user.home")+ LogConstants.LOG_FOLDER + LogConstants.ERR_FILE);
			if (fout.exists() && fout.length() > 0)
				files.add(fout);
			if (ferr.exists() && ferr.length() > 0)
				files.add(ferr);
		} else {
			// try to get the log file for module loads (/tmp/{user.name}-log.txt)
			File linuxLog = new File(System.getProperty("java.io.tmpdir") + System.lineSeparator() + System.getProperty("user.name") + "-log.txt");
			if (linuxLog.exists() && linuxLog.length() > 0) {
				files.add(linuxLog);
			} else {
				// try to get the log file in user.home
				linuxLog = new File(System.getProperty("user.home") + System.lineSeparator() + "dawnlog.html");
				if (linuxLog.exists() && linuxLog.length() > 0) {
					files.add(linuxLog);
				}
			}
		}
		return files;
	}

	private boolean isWindowsOS() {
		return (System.getProperty("os.name").indexOf("Windows") == 0);
	}
}
