package edu.usc.cssl.tacit.topicmodel.hlda.ui;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;

import edu.usc.cssl.tacit.common.Preprocessor;
import edu.usc.cssl.tacit.common.ui.composite.from.TacitFormComposite;
import edu.usc.cssl.tacit.common.ui.outputdata.OutputLayoutData;
import edu.usc.cssl.tacit.common.ui.outputdata.TableLayoutData;
import edu.usc.cssl.tacit.common.ui.utility.TacitUtil;
import edu.usc.cssl.tacit.common.ui.validation.OutputPathValidation;
import edu.usc.cssl.tacit.common.ui.views.ConsoleView;
import edu.usc.cssl.tacit.topicmodel.hlda.services.HLDA;
import edu.usc.cssl.tacit.topicmodel.hlda.ui.internal.HeirarchalLDAViewImageRegistry;
import edu.usc.cssl.tacit.topicmodel.hlda.ui.internal.IHeirarchicalLDAViewConstants;

public class HeirarchicalLDAView  extends ViewPart implements
		IHeirarchicalLDAViewConstants {
	public static final String ID = "edu.usc.cssl.tacit.topicmodel.hlda.ui.view1";
	private ScrolledForm form;
	private FormToolkit toolkit;
	private Button preprocessEnabled;

	private OutputLayoutData layoutData;
	private TableLayoutData inputLayoutData;
	private Text maxTopicLevel;
	private int wordsPerTopic;
	private int maxTopicHeirarchy;
	private Text numberOfWordsPerTopic;
	//private HierarchicalLDAModel hlda = new HierarchicalLDAModel();
	protected Job job;
	private boolean checkType = true;
	private List<Object> selectedFiles;
	private String outputDirectory;
	@Override
	public void createPartControl(Composite parent) {

		toolkit = createFormBodySection(parent);
		Section section = toolkit.createSection(form.getBody(),
				Section.TITLE_BAR | Section.EXPANDED);

		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(section);
		section.setExpanded(true);
		ScrolledComposite sc = new ScrolledComposite(section, SWT.H_SCROLL
				| SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false)
				.applyTo(sc);
		TacitFormComposite.addErrorPopup(form.getForm(), toolkit);
		TacitFormComposite.createEmptyRow(toolkit, sc);
		Composite client = toolkit.createComposite(form.getBody());
		GridLayoutFactory.fillDefaults().equalWidth(true).numColumns(1)
				.applyTo(client);
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1)
				.applyTo(client);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		// create input layout
		inputLayoutData = TacitFormComposite.createTableSection(client,
				toolkit, layout, "Input Details",
				"Add File(s) and Folder(s) to include in analysis.", true,
				true, true, true);
		Composite compInput;
		compInput = toolkit.createComposite(form.getBody());
		GridLayoutFactory.fillDefaults().equalWidth(false).numColumns(3)
				.applyTo(compInput);
		GridDataFactory.fillDefaults().grab(false, false).span(3, 1).indent(10, 0)
				.applyTo(compInput);
		createPreprocessLink(compInput);
		maxTopicLevel = createAdditionalOptions(compInput, "Max depth for topic heirarchy: ",
				"3");

		numberOfWordsPerTopic = createAdditionalOptions(compInput, "No. of Words per Topic: ",
				"10");
		
		Composite client1 = toolkit.createComposite(form.getBody());
		GridLayoutFactory.fillDefaults().equalWidth(true).numColumns(1)
				.applyTo(client1);
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1)
				.applyTo(client1);

		layoutData = TacitFormComposite.createOutputSection(toolkit, client1,
				form.getMessageManager());

		// we dont need stop word's as it will be taken from the preprocessor
		// settings


		form.getForm().addMessageHyperlinkListener(new HyperlinkAdapter());
		// form.setMessage("Invalid path", IMessageProvider.ERROR);
		this.setPartName("HLDA Topic Model");
		addButtonsToToolBar();
		toolkit.paintBordersFor(form.getBody());
		form.setImage(HeirarchalLDAViewImageRegistry.getImageIconFactory().getImage(IHeirarchicalLDAViewConstants.IMAGE_HEIRARCHICAL_LDA_OBJ));

	}

	private void createPreprocessLink(Composite client) {
		Composite clientLink = toolkit.createComposite(client);
		GridLayoutFactory.fillDefaults().equalWidth(false).numColumns(3)
				.applyTo(clientLink);
		GridDataFactory.fillDefaults().grab(false, false).span(3, 1)
				.applyTo(clientLink);

		preprocessEnabled = toolkit.createButton(clientLink, "", SWT.CHECK);
		GridDataFactory.fillDefaults().grab(false, false).span(1, 1)
				.applyTo(preprocessEnabled);
		final Hyperlink link = toolkit.createHyperlink(clientLink,
				"Preprocess", SWT.NONE);
		link.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		link.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				String id = "edu.usc.cssl.tacit.common.ui.prepocessorsettings";
				PreferencesUtil.createPreferenceDialogOn(link.getShell(), id,
						new String[] { id }, null).open();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(link);

	}

	private Text createAdditionalOptions(Composite sectionClient,
			String lblText, String defaultText) {
		Label simpleTxtLbl = toolkit.createLabel(sectionClient, lblText,
				SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0)
				.applyTo(simpleTxtLbl);
		Text simpleTxt = toolkit.createText(sectionClient, "", SWT.BORDER);
		simpleTxt.setText(defaultText);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 0)
				.applyTo(simpleTxt);
		return simpleTxt;
	}

	private FormToolkit createFormBodySection(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);

		toolkit.decorateFormHeading(form.getForm());
		form.setText("HLDA Topic Model"); //$NON-NLS-1$
		GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(true)
				.applyTo(form.getBody());
		return toolkit;
	}

	private void addButtonsToToolBar() {
		IToolBarManager mgr = form.getToolBarManager();
		mgr.add(new Action() {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return (HeirarchalLDAViewImageRegistry.getImageIconFactory()
						.getImageDescriptor(IMAGE_LRUN_OBJ));
			}

			@Override
			public String getToolTipText() {
				return "Analyze";
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.action.Action#run()
			 */
			@Override
			public void run() {
				if (!canProceedCluster()) {
					return;
				}
				
				final boolean isPreprocess = preprocessEnabled.getSelection();
				if(isPreprocess){
					checkType = false;
				}
				outputDirectory = layoutData.getOutputLabel().getText();
				TacitUtil tacitHelper = new TacitUtil();
				try {
					selectedFiles = inputLayoutData
							.getTypeCheckedSelectedFiles(checkType);
				} catch (Exception e2) {
					return;
				}
				tacitHelper.writeSummaryFile(outputDirectory);

			
				TacitFormComposite
						.writeConsoleHeaderBegining("Topic Modelling started  ");
				job = new Job("Analyzing...") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						TacitFormComposite.setConsoleViewInFocus();
						TacitFormComposite.updateStatusMessage(getViewSite(),
								null, null, form);
						monitor.beginTask("TACIT started analyzing...", 120);
						String topicModelDirPath = System
								.getProperty("user.dir")
								+ File.separator
								+ "TopicModel"
								+ "_"
								+ String.valueOf(System.currentTimeMillis());
						Preprocessor ppObj = null;
						List<String> inFiles;
						try {
							ppObj = new Preprocessor("HLDA", isPreprocess);
							inFiles = ppObj.processData("HLDA", selectedFiles, monitor);

							for (String filename : inFiles) {
								File srcFile = new File(filename);
								File destDir = new File(topicModelDirPath);
								try {
									FileUtils.copyFileToDirectory(srcFile,
											destDir, false);
								} catch (IOException e) {
									e.printStackTrace();
									return Status.CANCEL_STATUS;
								}
							}
						} catch (IOException e1) {
							e1.printStackTrace();
							return Status.CANCEL_STATUS;
						} catch (Exception e1) {
							e1.printStackTrace();
							return Status.CANCEL_STATUS;
						}
						

						//lda.initialize(topicModelDirPath, noOfTopics,
						//		outputPath, preFix, wordWeightFile);

						// lda processsing
						long startTime = System.currentTimeMillis();
						Date dateObj = new Date();
						monitor.subTask("Topic Modelling...");
						File directory = new File(topicModelDirPath);
						
						monitor.worked(5);
						HLDA hlda = new HLDA(outputDirectory, wordsPerTopic, maxTopicHeirarchy, monitor);
						hlda.buildPipe();
						hlda.readDirectory(directory);
						monitor.worked(10);
						try {
							hlda.runHLDA();
						} catch (IOException e1) {
							return Status.CANCEL_STATUS;
						}
						
						monitor.worked(5);
						
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						ConsoleView
								.printlInConsoleln("HLDA Topic Modelling completed successfully in "
										+ (System.currentTimeMillis() - startTime)
										+ " milliseconds.");

						ppObj.clean();
						try {
							FileUtils.deleteDirectory(new File(
									topicModelDirPath));
						} catch (IOException e) {
							e.printStackTrace();
						}
						monitor.worked(10);
						monitor.done();
						return Status.OK_STATUS;
					}
				};
				job.setUser(true);
				if (canProceedCluster()) {
					job.schedule();
					job.addJobChangeListener(new JobChangeAdapter() {

						@Override
						public void done(IJobChangeEvent event) {
							if (!event.getResult().isOK()) {
								TacitFormComposite
										.writeConsoleHeaderBegining("Error: <Terminated> HLDA Topic Modelling");
								ConsoleView
										.printlInConsoleln("LDA not successful.");
							} else {
								TacitFormComposite.updateStatusMessage(
										getViewSite(),
										"LDA topic modelling completed",
										IStatus.OK, form);

								TacitFormComposite
										.writeConsoleHeaderBegining("Success: <Completed> HLDA Topic Modelling ");

							}
						}
					});
				} else {
					TacitFormComposite
							.updateStatusMessage(
									getViewSite(),
									"HLDA Topic Modelling cannot be started. Please check the Form status to correct the errors",
									IStatus.ERROR, form);
				}

			}

		});
		Action helpAction = new Action() {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return (HeirarchalLDAViewImageRegistry.getImageIconFactory()
						.getImageDescriptor(IMAGE_HELP_CO));
			}

			@Override
			public String getToolTipText() {
				return "Help";
			}

			@Override
			public void run() {
				PlatformUI
						.getWorkbench()
						.getHelpSystem()
						.displayHelp("edu.usc.cssl.tacit.topicmodel.lda.ui.lda");
			};
		};
		mgr.add(helpAction);
		PlatformUI
				.getWorkbench()
				.getHelpSystem()
				.setHelp(helpAction, "edu.usc.cssl.tacit.topicmodel.lda.ui.lda");
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(form, "edu.usc.cssl.tacit.topicmodel.lda.ui.lda");
		form.getToolBarManager().update(true);
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

	private boolean canProceedCluster() {
		TacitFormComposite.updateStatusMessage(getViewSite(), null, null, form);
		boolean canProceed = true;
		form.getMessageManager().removeMessage("location");
		form.getMessageManager().removeMessage("inputlocation");
		form.getMessageManager().removeMessage("prefix");
		form.getMessageManager().removeMessage("topic");
		String message = OutputPathValidation.getInstance()
				.validateOutputDirectory(layoutData.getOutputLabel().getText(),
						"Output");
		if (message != null) {

			message = layoutData.getOutputLabel().getText() + " " + message;
			form.getMessageManager().addMessage("location", message, null,
					IMessageProvider.ERROR);
			canProceed = false;
		}
		// validate input
		try {
			if (inputLayoutData.getTypeCheckedSelectedFiles(checkType).size() < 1) {
				form.getMessageManager().addMessage("input",
						"Select/Add atleast one input file", null,
						IMessageProvider.ERROR);
				canProceed = false;
			}
		} catch (Exception e) {
			canProceed = false;
		}

		if (maxTopicLevel.getText().isEmpty()
				|| Integer.parseInt(maxTopicLevel.getText()) < 1) {
			form.getMessageManager().addMessage("topic",
					"Number of topics cannot be empty or less than 1", null,
					IMessageProvider.ERROR);
			canProceed = false;
		}


		wordsPerTopic = Integer.parseInt(numberOfWordsPerTopic.getText());
		maxTopicHeirarchy = Integer.parseInt(maxTopicLevel.getText());
		return canProceed;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == Job.class) {
			return job;
		}
		return super.getAdapter(adapter);
	}

}
