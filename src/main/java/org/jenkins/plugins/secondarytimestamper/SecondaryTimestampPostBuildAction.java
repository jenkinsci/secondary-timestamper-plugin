package org.jenkins.plugins.secondarytimestamper;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;


public class SecondaryTimestampPostBuildAction extends Recorder implements MatrixAggregatable
{
	private final String timezone;

	@DataBoundConstructor
	public SecondaryTimestampPostBuildAction(String timezone)
	{
		this.timezone = timezone;
		getDescriptor().setSelectedTimezone(timezone);
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException
	{
		Date date = new Date();
		int offsetCurrentTimeZone = TimeZone.getDefault().getOffset(date.getTime());
		int offsetNewTimeZone = TimeZone.getTimeZone(this.timezone).getOffset(date.getTime());
		long timeNewTimeZone = build.getStartTimeInMillis() - (long) offsetCurrentTimeZone + (long) offsetNewTimeZone;
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone(this.timezone));
		calendar.setTimeInMillis(timeNewTimeZone);

		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM d, yyyy hh:mm:ss aaa");
		String dateString = simpleDateFormat.format(calendar.getTime()); 
//		simpleDateFormat.s
		
		build.setDescription(this.timezone + " " + dateString);
		return true;
//		return DescriptionSetterHelper.setDescription(build, listener, "", "");
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>
	{
		private String selectedTimezone = "";
		
		public DescriptorImpl()
		{
			super(SecondaryTimestampPostBuildAction.class);
		}

		@Override
		public String getDisplayName()
		{
			return Messages.SecondTimestampPostBuildAction_DisplayName();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType)
		{
			return true;
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData)
				throws FormException
		{
			return req.bindJSON(SecondaryTimestampPostBuildAction.class, formData);
		}
		
		public ListBoxModel doFillTimezoneItems()
		{
			ListBoxModel timezones = new ListBoxModel();
			String[] availableTimezones = TimeZone.getAvailableIDs();
			Arrays.sort(availableTimezones);
			for(String timezoneID : availableTimezones)
			{
				timezones.add(new Option(timezoneID, timezoneID, this.selectedTimezone.equals(timezoneID)));
			}

			return timezones;
		}
		
		public void setSelectedTimezone(String selectedTimezone)
		{
			this.selectedTimezone = selectedTimezone;
		}

	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public String getTimezone()
	{
		return timezone;
	}
	
	public MatrixAggregator createAggregator(final MatrixBuild build, Launcher launcher, final BuildListener listener)
	{
		return new MatrixAggregator(build, launcher, listener)
		{
			@Override
			public boolean endRun(MatrixRun run) throws InterruptedException, IOException
			{
				if (build.getDescription() == null && run.getDescription() != null)
				{
					build.setDescription(run.getDescription());
				}
			return true;
			}
		};
	}
	
}
