package info.xuluan.podcast.utils;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockHandler {
	
	private ReentrantReadWriteLock lock;
	private Boolean status;
	
	public LockHandler()
	{
		lock = new ReentrantReadWriteLock();
		status = false;
	}

	public boolean getStatus()
	{
		return status;
	}
	
	public boolean locked()
	{
		lock.readLock().lock();
		if (status) {
			lock.readLock().unlock();
			return false;

		}
		lock.readLock().unlock();

		lock.writeLock().lock();
		status = true;
		lock.writeLock().unlock();
		
		return true;
	}
	
	public void release()
	{
		lock.writeLock().lock();
		status = false;
		lock.writeLock().unlock();
	}	

		

}
