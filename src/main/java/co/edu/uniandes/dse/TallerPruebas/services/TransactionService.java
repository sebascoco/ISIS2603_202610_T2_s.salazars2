package co.edu.uniandes.dse.TallerPruebas.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniandes.dse.TallerPruebas.entities.AccountEntity;
import co.edu.uniandes.dse.TallerPruebas.entities.PocketEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import co.edu.uniandes.dse.TallerPruebas.repositories.AccountRepository;
import co.edu.uniandes.dse.TallerPruebas.repositories.PocketRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransactionService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PocketRepository pocketRepository;

    @Transactional
    public void transferToPocket(Long accountId, Long pocketId, Double monto)
            throws EntityNotFoundException, BusinessLogicException {

        log.info("Inicia proceso de transferencia de cuenta {} a bolsillo {}", accountId, pocketId);

        Optional<AccountEntity> accountEntity = accountRepository.findById(accountId);
        if (accountEntity.isEmpty()) {
            throw new EntityNotFoundException("La cuenta no existe");
        }

        AccountEntity account = accountEntity.get();

        if (!"ACTIVA".equals(account.getEstado())) {
            throw new BusinessLogicException("No se pueden realizar transferencias porque la cuenta está bloqueada");
        }

        Optional<PocketEntity> pocketEntity = pocketRepository.findById(pocketId);
        if (pocketEntity.isEmpty()) {
            throw new EntityNotFoundException("El bolsillo no existe");
        }

        PocketEntity pocket = pocketEntity.get();

        if (!pocket.getAccount().getId().equals(accountId)) {
            throw new BusinessLogicException("El bolsillo no pertenece a la cuenta indicada");
        }

        if (monto == null || monto <= 0) {
            throw new BusinessLogicException("El monto debe ser mayor a cero");
        }

        if (account.getSaldo() < monto) {
            throw new BusinessLogicException("Saldo insuficiente para realizar la transferencia");
        }

        account.setSaldo(account.getSaldo() - monto);
        pocket.setSaldo(pocket.getSaldo() + monto);

        accountRepository.save(account);
        pocketRepository.save(pocket);

        log.info("Finaliza proceso de transferencia de cuenta {} a bolsillo {}", accountId, pocketId);
    }

    @Transactional
    public void transferToAccount(Long originAccountId, Long destinationAccountId, Double monto)
            throws EntityNotFoundException, BusinessLogicException {

        log.info("Inicia transferencia de cuenta {} a cuenta {}", originAccountId, destinationAccountId);

        // 1. Validar que el monto sea mayor a cero
        if (monto == null || monto <= 0) {
            throw new BusinessLogicException("El monto debe ser mayor a cero");
        }

        // 2. Validar que las cuentas no sean la misma
        if (originAccountId.equals(destinationAccountId)) {
            throw new BusinessLogicException("La cuenta de origen no puede ser la misma que la cuenta destino");
        }

        // 3. Verificar que la cuenta de origen existe
        Optional<AccountEntity> originEntity = accountRepository.findById(originAccountId);
        if (originEntity.isEmpty()) {
            throw new EntityNotFoundException("La cuenta de origen no existe");
        }

        // 4. Verificar que la cuenta destino existe
        Optional<AccountEntity> destinationEntity = accountRepository.findById(destinationAccountId);
        if (destinationEntity.isEmpty()) {
            throw new EntityNotFoundException("La cuenta destino no existe");
        }

        AccountEntity originAccount = originEntity.get();
        AccountEntity destinationAccount = destinationEntity.get();

        // 5. Validar que ambas cuentas estén activas
        if (!"ACTIVA".equals(originAccount.getEstado())) {
            throw new BusinessLogicException("La cuenta de origen está bloqueada o inactiva");
        }

        if (!"ACTIVA".equals(destinationAccount.getEstado())) {
            throw new BusinessLogicException("La cuenta destino está bloqueada o inactiva");
        }

        // 6. Validar fondos suficientes en la cuenta de origen
        if (originAccount.getSaldo() < monto) {
            throw new BusinessLogicException("La cuenta de origen no tiene fondos suficientes");
        }

        // 7. Actualizar saldos
        originAccount.setSaldo(originAccount.getSaldo() - monto);
        destinationAccount.setSaldo(destinationAccount.getSaldo() + monto);

        // 8. Guardar cambios
        accountRepository.save(originAccount);
        accountRepository.save(destinationAccount);

        log.info("Finaliza transferencia de cuenta {} a cuenta {}", originAccountId, destinationAccountId);
    }
}
