/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Abel Hegedus - initial API and implementation
 *******************************************************************************/
package hu.bme.mit.documentation.ecore.ui.handlers;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import hu.bme.mit.documentation.generator.ecore.IDocGenerator;
import hu.bme.mit.documentation.generator.ecore.UtilDocGenerator;

/**
 * @author Abel Hegedus, Adam Horvath
 * 
 */
public abstract class AbstractGenerateEcoreDoc extends AbstractHandler {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands. ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {

            for (Object element : ((IStructuredSelection) selection).toList()) {
                if (element instanceof IFile) {
                    IFile file = (IFile) element;
                    if(file.getFileExtension().equals("ecore") || file.getFileExtension().equals("xcore")){
                        ResourceSet model = loadModel(file);
                        
                        String ecoreFileName = file.getName().substring(0,file.getName().indexOf("."));
                        String outputFileName = ecoreFileName+"."+getFileExtension();
                        String filterFileName = ecoreFileName+".docgen";
                        final IFile outFile;
                        final IFile filterFile;
                        
                        IContainer parent = file.getParent();
                        
                        if(parent instanceof IProject){
                            IProject project = (IProject) parent;
                            outFile = project.getFile(outputFileName);
                            filterFile = project.getFile(filterFileName);
                        } else if(parent instanceof IFolder) {
                            IFolder folder = (IFolder) parent;
                            outFile = folder.getFile(outputFileName);
                            filterFile = folder.getFile(filterFileName);
                        } else {
                        	outFile = null;
                        	filterFile = null;
                        }
                        
                        final IDocGenerator docGen = getCodeGenerator();
                        
                        docGen.setOutputFile(outFile.getLocation().toFile());
                        
                        UtilDocGenerator.generateDocForResourceSet(model, 
                        		new File(outFile.getLocationURI()), 
                        		new File(filterFile.getLocationURI()),
                        		docGen);
                    }
                }
                else if (element instanceof IFolder)
                {
                	IFolder folder = (IFolder) element;
                    ResourceSet model = loadModel(folder);
                    
                    String ecoreFileName = folder.getName();
                    String outputFileName = ecoreFileName+"."+getFileExtension();
                    String filterFileName = ecoreFileName+".docgen";
                    final IFile outFile;
                    final IFile filterFile;
                    
                    IContainer parent = folder.getParent();
                    
                    if(parent instanceof IProject){
                        IProject project = (IProject) parent;
                        outFile = project.getFile(outputFileName);
                        filterFile = project.getFile(filterFileName);
                    } else if(parent instanceof IFolder) {
                        IFolder parentFolder = (IFolder) parent;
                        outFile = parentFolder.getFile(outputFileName);
                        filterFile = parentFolder.getFile(filterFileName);
                    } else {
                    	outFile = null;
                    	filterFile = null;
                    }
                    
                    final IDocGenerator docGen = getCodeGenerator();
                    
                    docGen.setOutputFile(outFile.getLocation().toFile());
                    
                    UtilDocGenerator.generateDocForResourceSet(model, 
                    		new File(outFile.getLocationURI()), 
                    		new File(filterFile.getLocationURI()),
                    		docGen);
                
                }
                
            }
        }

        return null;
    }
    
    protected ResourceSet loadModel(IFile file){
        ResourceSet set = UtilDocGenerator.newResourceSet();
        URI ecoreURI = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
        set.getResource(ecoreURI, true);
        return set;
    }
    
    protected ResourceSet loadModel(IFolder folder){
        ResourceSet set = UtilDocGenerator.newResourceSet();
        
        addModelsToResourceSet(set, folder);
        
        return set;
    }
    
    void addModelsToResourceSet(ResourceSet resourceSet, IResource resource)
    {
    	if( resource instanceof IFile)
    	{
    		IFile file = (IFile)resource;
    		if(file.getFileExtension().equals("ecore") || file.getFileExtension().equals("xcore"))
    		{
	    		URI ecoreURI = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
	    		resourceSet.getResource(ecoreURI, true);
    		}
    	}
    	else if( resource instanceof IFolder)
    	{
    		IFolder folder = (IFolder)resource;
    		try {
				for( IResource child : folder.members() )
					addModelsToResourceSet(resourceSet, child);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    }
    
    
    /**
	 * Should be overridden by subclasses to allow documentation generation in
	 * specific formats.
	 * 
	 * @return the documentation generator to use
	 */
	protected abstract IDocGenerator getCodeGenerator();
	
	/**
	 * Should be overridden by subclasses to specify the file extension for the generated documentation.
	 * @return the file extension (without preceeding ".")
	 */
	protected abstract String getFileExtension();

}
