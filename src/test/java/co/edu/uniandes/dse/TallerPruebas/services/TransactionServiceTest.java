package co.edu.uniandes.dse.TallerPruebas.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import co.edu.uniandes.dse.TallerPruebas.entities.AccountEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(TransactionService.class)
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TestEntityManager entityManager;

    private PodamFactory factory = new PodamFactoryImpl();

    private List<AccountEntity> accountList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clearData();
        insertData();
    }

    private void clearData() {
        entityManager.getEntityManager().createQuery("delete from AccountEntity").executeUpdate();
    }

    private void insertData() {
        for (int i = 0; i < 3; i++) {
            AccountEntity account = factory.manufacturePojo(AccountEntity.class);
            account.setEstado("ACTIVA");
            account.setSaldo(1000.0);
            entityManager.persist(account);
            accountList.add(account);
        }
    }

    @Test
    void testTransferToAccount() throws EntityNotFoundException, BusinessLogicException {

        AccountEntity origin = accountList.get(0);
        AccountEntity destination = accountList.get(1);

        Double monto = 200.0;

        transactionService.transferToAccount(origin.getId(), destination.getId(), monto);

        AccountEntity updatedOrigin = entityManager.find(AccountEntity.class, origin.getId());
        AccountEntity updatedDestination = entityManager.find(AccountEntity.class, destination.getId());

        assertEquals(800.0, updatedOrigin.getSaldo());
        assertEquals(1200.0, updatedDestination.getSaldo());
    }

    @Test
    void testTransferWithInvalidOriginAccount() {
        assertThrows(EntityNotFoundException.class, () -> {
            transactionService.transferToAccount(0L, accountList.get(1).getId(), 100.0);
        });
    }

    @Test
    void testTransferWithInvalidDestinationAccount() {
        assertThrows(EntityNotFoundException.class, () -> {
            transactionService.transferToAccount(accountList.get(0).getId(), 0L, 100.0);
        });
    }

    @Test
    void testTransferWithBlockedOriginAccount() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity origin = accountList.get(0);
            origin.setEstado("BLOQUEADA");
            entityManager.merge(origin);

            transactionService.transferToAccount(origin.getId(), accountList.get(1).getId(), 100.0);
        });
    }

    @Test
    void testTransferWithBlockedDestinationAccount() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity destination = accountList.get(1);
            destination.setEstado("BLOQUEADA");
            entityManager.merge(destination);

            transactionService.transferToAccount(accountList.get(0).getId(), destination.getId(), 100.0);
        });
    }

    @Test
    void testTransferWithSameAccount() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity account = accountList.get(0);
            transactionService.transferToAccount(account.getId(), account.getId(), 100.0);
        });
    }

    @Test
    void testTransferWithInsufficientFunds() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity origin = accountList.get(0);
            origin.setSaldo(50.0);
            entityManager.merge(origin);

            transactionService.transferToAccount(origin.getId(), accountList.get(1).getId(), 100.0);
        });
    }

    @Test
    void testTransferWithInvalidAmount() {
        assertThrows(BusinessLogicException.class, () -> {
            transactionService.transferToAccount(accountList.get(0).getId(), accountList.get(1).getId(), -100.0);
        });
    }
}
