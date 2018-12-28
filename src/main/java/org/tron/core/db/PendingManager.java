package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionTrace.TimeResultType;

@Slf4j(topic = "DB")
public class PendingManager implements AutoCloseable {

  @Getter
  static List<TransactionCapsule> tmpTransactions = new ArrayList<>();

  Manager dbManager;

  private long maxRunningTime = Args.getInstance().getLongRunningTime();

  public PendingManager(Manager db) {

    this.dbManager = db;
    tmpTransactions.addAll(db.getPendingTransactions());
    db.getPendingTransactions().clear();
    db.getSession().reset();
  }

  @Override
  public void close() {
    logger.info("*** 1 RePushTransactions size: {}, pending size: {}, poped size: {}",
        dbManager.getRepushTransactions().size(),
        PendingManager.tmpTransactions.size(),
        dbManager.getPoppedTransactions().size());
    for (TransactionCapsule tx : PendingManager.tmpTransactions) {
      try {
        if (tx.getCost() <= maxRunningTime) {
          dbManager.getRepushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    tmpTransactions.clear();

    for (TransactionCapsule tx : dbManager.getPoppedTransactions()) {
      try {
        if (tx.getCost() <= maxRunningTime) {
          dbManager.getRepushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    dbManager.getPoppedTransactions().clear();
    logger.info("*** 2 RePushTransactions size: {}, pending size: {}, poped size: {}",
        dbManager.getRepushTransactions().size(),
        PendingManager.tmpTransactions.size(),
        dbManager.getPoppedTransactions().size());
  }
}
