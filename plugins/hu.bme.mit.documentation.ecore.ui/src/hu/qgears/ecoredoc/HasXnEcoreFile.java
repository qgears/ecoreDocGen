package hu.qgears.ecoredoc;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class HasXnEcoreFile extends PropertyTester {
		
	@Override
	public boolean test(Object arg0, String arg1, Object[] arg2, Object arg3) {
		IFolder folder = (IFolder)arg0;
		return OnlyXnECore(folder) && HasXnECore(folder);
	}
	
	private boolean XnECore( IFile file )
	{
		String name = file.getName();
		int nameLen = name.length();
		if(nameLen < 6)
			return false;
		
		return name.charAt(nameLen-6)=='.' && "core".equals(name.substring(nameLen-4, nameLen));
	}
	private boolean OnlyXnECore( IFolder folder )
	{
		try {
			for( IResource member: folder.members() )
			{
				if(member instanceof IFolder && !OnlyXnECore((IFolder)member))
					return false;
				else
					if(member instanceof IFile && !XnECore((IFile)member) )
						return false;
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	private boolean HasXnECore( IFolder folder )
	{
		try {
			for( IResource member: folder.members() )
			{
				if(member instanceof IFolder && HasXnECore((IFolder)member))
					return true;
				else
					if(member instanceof IFile && XnECore((IFile)member) )
						return true;
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

}
